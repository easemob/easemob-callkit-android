package com.hyphenate.callkit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hyphenate.callkit.CallKitClient
import com.hyphenate.callkit.R
import com.hyphenate.callkit.base.BaseCallActivity
import com.hyphenate.callkit.bean.CallState
import com.hyphenate.callkit.bean.CallType
import com.hyphenate.callkit.ui.MultiCallActivity
import com.hyphenate.callkit.ui.SingleCallActivity
import com.hyphenate.callkit.utils.CallKitUtils
import com.hyphenate.callkit.utils.ChatLog
import com.hyphenate.callkit.utils.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


/**
 * \~chinese
 * 通话前台服务
 * 用于在应用后台时保持摄像头和麦克风权限，确保视频通话正常进行
 *
 * \~english
 * Call foreground service
 * Used to keep the camera and microphone permissions when the application is in the background, ensuring normal video calls
 */
class CallForegroundService : Service() {

    companion object {
        private const val TAG = "CallForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "call_foreground_service"
        private const val CHANNEL_NAME = "通话服务"

        // 标记服务是否已完成 startForeground() 调用
        @Volatile
        var isForegroundStarted = false
            private set

        // 标记是否有待处理的停止请求
        @Volatile
        private var pendingStop = false

        private const val ACTION_LAUNCH_ACTIVITY = "LAUNCH_ACTIVITY"
        private const val ACTION_END_CALL = "END_CALL"

        /**
         * 启动前台服务
         * @param launchActivity 是否同时启动通话Activity（用于telecom接听场景）
         */
        @JvmOverloads
        fun startService(context: Context, launchActivity: Boolean = false) {
            // 普通场景下检查是否在通话中
            if (!launchActivity && CallKitClient.callState.value == CallState.CALL_IDLE) {
                return
            }
            pendingStop = false
            try {
                val intent = Intent(context, CallForegroundService::class.java).apply {
                    if (launchActivity) action = ACTION_LAUNCH_ACTIVITY
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                ChatLog.e(TAG, "Failed to start service: ${e.message}")
            }
        }
        
        /**
         * \~chinese
         * 停止前台服务
         *
         * \~english
         * Stop foreground service
         */
        fun stopService(context: Context) {
            if (isForegroundStarted) {
                // 服务已完成 startForeground()，可以安全停止
                val intent = Intent(context, CallForegroundService::class.java)
                context.stopService(intent)
                isForegroundStarted = false
                pendingStop = false
            } else {
                // 服务还未完成 startForeground()，标记待停止
                // 服务启动完成后会检查此标记并自行停止
                ChatLog.d(TAG, "Service not yet started foreground, marking pending stop")
                pendingStop = true
            }
        }
    }

    private var serviceScope: CoroutineScope? = null
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        // 创建通知渠道
        createNotificationChannel()

        // 立即启动前台服务以避免超时异常（必须在5秒内调用）
        val notification = createNotification()

        try {
            // 根据 Android 版本，选择合适的方式处理前台服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 对于 Android 11 及以上版本，启动前台服务并指定多种服务类型
                val serviceTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                this.startForeground(NOTIFICATION_ID, notification, serviceTypes)
            } else {
                // 对于 Android 11 以下版本，无需指定服务类型，简单地启动前台服务即可
                this.startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundStarted = true
            ChatLog.d(TAG, "successful startForeground")

            // 检查是否有待处理的停止请求
            if (pendingStop) {
                ChatLog.d(TAG, "Pending stop detected, stopping service now")
                pendingStop = false
                stopSelf()
                return
            }
        } catch (ex: java.lang.Exception) {
            ChatLog.e(TAG, "Error starting foreground service:" + ex)
        }
        // 检查权限（在启动前台服务之后）
        checkRequiredPermissions()

        // 检查电池优化状态
        checkBatteryOptimization()

        // 创建服务协程作用域
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // 开始观察通话状态
        startObservingCallState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // 处理特殊动作
        when (intent?.action) {
            ACTION_END_CALL -> {
                // 结束通话
                CallKitClient.exitCall()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_LAUNCH_ACTIVITY -> {
                // 这里可以启动通话中的界面
                CallKitClient.signalingManager.startSendEvent()
                launchCallActivityFromService()
                return START_STICKY
            }
        }

        // 更新通知内容（如果需要）
        updateNotification()

        return START_STICKY // 服务被杀死后自动重启
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        // 重置前台服务状态标志
        isForegroundStarted = false
        pendingStop = false

        // 取消观察
        observeJob?.cancel()

        // 取消协程作用域
        serviceScope?.cancel()
    }

    /**
     * 从前台服务启动通话 Activity，避免被系统后台启动限制拦截（如小米 8 从通知栏接听后无法调起接听页）
     *
     * Android 10+ 严格限制后台启动 Activity（BAL），前台服务并不能豁免：
     * - 应用在前台，或已授予悬浮窗权限（SYSTEM_ALERT_WINDOW 可豁免 BAL）时，直接 startActivity；
     * - 否则发送带 fullScreenIntent 的通话通知，由系统全屏拉起通话界面（锁屏/灭屏时自动弹出，
     *   亮屏使用时显示为顶部悬浮通知，点击进入通话界面）。
     */
    private fun launchCallActivityFromService() {
        try {
            val callType = CallKitClient.callType.value
            val activityClass = if (callType == CallType.GROUP_CALL) {
                MultiCallActivity::class.java
            } else {
                SingleCallActivity::class.java
            }
            val intent = BaseCallActivity.createLockScreenIntent(this, activityClass)
            if (CallKitUtils.isAppRunningForeground(this) ||
                PermissionHelper.hasFloatWindowPermission(this)
            ) {
                startActivity(intent)
                ChatLog.d(TAG, "Launched call activity from foreground service: ${activityClass.simpleName}")
            } else {
                val content = when (callType) {
                    CallType.SINGLE_VIDEO_CALL -> "视频通话 • 点击进入通话界面"
                    CallType.SINGLE_VOICE_CALL -> "语音通话 • 点击进入通话界面"
                    CallType.GROUP_CALL -> "多人通话 • 点击进入通话界面"
                }
                CallKitClient.notifier.notify(intent, null, content)
                ChatLog.d(
                    TAG,
                    "Posted full-screen intent notification to launch: ${activityClass.simpleName}"
                )
            }
        } catch (e: Exception) {
            ChatLog.e(TAG, "Failed to launch call activity from service: ${e.message}")
        }
    }

    /**
     * 检查必需权限
     */
    private fun checkRequiredPermissions(): Boolean {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ChatLog.e(TAG, "Missing permission: $permission")
                return false
            }
        }
        return true
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持通话服务在后台运行"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        // 安全获取通话状态信息，避免在初始化时出现异常
        val callState = try {
            CallKitClient.callState.value
        } catch (e: Exception) {
            ChatLog.w(TAG, "Failed to get call state: ${e.message}")
            CallState.CALL_IDLE
        }

        val callType = try {
            CallKitClient.callType.value
        } catch (e: Exception) {
            ChatLog.w(TAG, "Failed to get call type: ${e.message}")
            CallType.SINGLE_VIDEO_CALL
        }

        val duration = try {
            CallKitClient.rtcManager.connectedTime.value
        } catch (e: Exception) {
            0L
        }

        // 根据通话类型创建合适的Intent
        val intent = when (callType) {
            CallType.GROUP_CALL -> BaseCallActivity.createLockScreenIntent(
                this,
                MultiCallActivity::class.java
            )

            else -> BaseCallActivity.createLockScreenIntent(this, SingleCallActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (callState) {
            CallState.CALL_ANSWERED -> {
                val minutes = duration / 60
                val seconds = duration % 60
                "通话中 ${String.format("%02d:%02d", minutes, seconds)}"
            }

            CallState.CALL_OUTGOING -> "拨打中"
            CallState.CALL_ALERTING -> "响铃中"
            else -> "通话服务"
        }

        val content = when (callType) {
            CallType.SINGLE_VIDEO_CALL -> "视频通话 • 点击返回通话界面"
            CallType.SINGLE_VOICE_CALL -> "语音通话 • 点击返回通话界面"
            CallType.GROUP_CALL -> "多人通话 • 点击返回通话界面"
        }

        // 添加操作按钮
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.callkit_phone_pick)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 不可滑动删除
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 提高优先级
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setUsesChronometer(callState == CallState.CALL_ANSWERED) // 通话中显示计时器

        // 如果是通话中状态，添加结束通话按钮
        if (callState == CallState.CALL_ANSWERED) {
            val endCallIntent = Intent(this, CallForegroundService::class.java).apply {
                action = ACTION_END_CALL
            }
            val endCallPendingIntent = PendingIntent.getService(
                this,
                1,
                endCallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "结束通话",
                endCallPendingIntent
            )
        }

        return builder.build()
    }

    /**
     * \~chinese
     * 开始观察通话状态
     *
     * \~english
     * Start observing call state
     */
    private fun startObservingCallState() {
        observeJob = serviceScope?.launch {
            try {
                CallKitClient.callState.collect { callState ->

                    when (callState) {
                        CallState.CALL_IDLE -> {
                            // 通话结束，先移除前台通知再停止服务，避免通知残留
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                stopForeground(Service.STOP_FOREGROUND_REMOVE)
                            } else {
                                @Suppress("DEPRECATION")
                                stopForeground(true)
                            }
                            stopSelf()
                        }

                        else -> {
                            // 更新通知
                            updateNotification()
                        }
                    }
                }
            } catch (e: Exception) {
                ChatLog.e(TAG, "Error observing call state: ${e.message}")
            }
        }
    }

    /**
     * \~chinese
     * 更新通知
     *
     * \~english
     * Update notification
     */
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * \~chinese
     * 检查电池优化状态
     *
     * \~english
     * Check battery optimization status
     */
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringBatteryOptimizations =
                powerManager.isIgnoringBatteryOptimizations(packageName)

            if (!isIgnoringBatteryOptimizations) {
                ChatLog.w(TAG, "App is not in battery optimization whitelist")
            }
        }
    }
} 