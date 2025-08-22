package com.hyphenate.callkit.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat
import com.hyphenate.callkit.CallKitClient
import com.hyphenate.callkit.R
import com.hyphenate.callkit.telecom.PhoneAccountHelper.getPhoneAccountHandle
import com.hyphenate.callkit.utils.ChatLog
import java.util.UUID
import androidx.core.net.toUri

/**
 * \~chinese
 * 来电服务
 *
 * \~english
 * Incoming call service
 */
class IncomingCallService : Service() {

    // 兼容旧版本的常量
    companion object {
        private val TAG = "IncomingCallService"
        const val EXTRA_CALLER_DISPLAY_NAME_COMPAT =
            "android.telecom.extra.CALLER_DISPLAY_NAME"

        /**
         * \~chinese
         * 启动前台服务
         *
         * \~english
         * Start foreground service
         */
        @Throws(Exception::class)
        fun startService(context: Context, callerId: String, callerName: String) {
            val intent = Intent(context, IncomingCallService::class.java).apply {
                action = "INCOMING_CALL"
                putExtra("callerId", callerId)
                putExtra("callerName", callerName)
                putExtra("callId", "call_${System.currentTimeMillis()}")
            }
            // Android 8.0+ 需要使用前台服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            ChatLog.d("TelecomHelper", "Service started successfully")
        }

        /**
         * \~chinese
         * 停止前台服务
         *
         * \~english
         * Stop foreground service
         */
        fun stopService(context: Context) {
            val intent = Intent(context, IncomingCallService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var telecomManager: TelecomManager

    override fun onCreate() {
        super.onCreate()
        ChatLog.d(TAG, "IncomingCallService onCreate")

        // 立即启动前台服务（Android 8.0+ 必需）
        startForegroundService()
        telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
    }

    private fun startForegroundService() {
        try {
            // 先创建通知渠道（Android 8.0+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "voip_channel",
                    "VoIP Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "VoIP service notification "
                    setShowBadge(false)
                    setSound(null, null)
                }
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            // 创建通知
            val notification = NotificationCompat.Builder(this, "voip_channel")
                .setContentTitle("VoIP Service running")
                .setSmallIcon(R.drawable.callkit_phone_pick)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()

            // 启动前台服务
            startForeground(1, notification)
            ChatLog.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            ChatLog.e(TAG, "Failed to start foreground service: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理 FCM 触发的来电
        if (intent?.action == "INCOMING_CALL") {
            val callerId = intent.getStringExtra("callerId") ?: "Unknown number"
            val callerName = intent.getStringExtra("callerName") ?: "Unknown contact"
            val callId = intent.getStringExtra("callId") ?: UUID.randomUUID().toString()

            handleIncomingCall(callerId, callerName, callId)
        }
        return START_STICKY
    }

    /**
     * \~chinese
     * 处理来电
     *
     * \~english
     * Handle incoming call
     */
    private fun handleIncomingCall(callerId: String, callerName: String, callId: String) {
        // 检查设备兼容性
        val status = PhoneAccountHelper.getPhoneAccountStatus(this)
        if (!status.isSupported or !status.isRegistered or !status.isEnabled) {
            ChatLog.e(
                TAG,
                "Phone account is not supported or not registered or not enabled: ${status.message}"
            )
            // 如果PhoneAccount未启用，直接启动自定义来电界面
            startCustomIncomingCallActivity(callerId, callerName, callId)
            return
        }

        // 创建来电参数
        val extras = Bundle().apply {
            val uri = "tel:$callerId".toUri()
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri)
            putString(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS, callerName)
            putString("call_id", callId)
            putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
            putString(TelecomManager.EXTRA_CALL_SUBJECT, callerName)
        }

        // 触发系统来电界面
        try {
            ChatLog.d(TAG, "Attempting to add incoming call with extras: $extras")
            telecomManager.addNewIncomingCall(getPhoneAccountHandle(this), extras)
            ChatLog.d(
                TAG,
                "Incoming call added successfully: $callerName ($callerId), Call ID: $callId"
            )
        } catch (e: Exception) {
            ChatLog.e(TAG, "Failed to add incoming call: ${e.message}")
            startCustomIncomingCallActivity(callerId, callerName, callId)
        }
    }

    private fun startCustomIncomingCallActivity(
        callerId: String,
        callerName: String,
        callId: String
    ) {
        ChatLog.d(TAG, "Starting custom incoming call activity as fallback")
        CallKitClient.signalingManager.startSendEvent()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}