package com.hyphenate.callkit.telecom

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.hyphenate.callkit.CallKitClient
import com.hyphenate.callkit.utils.ChatLog

/**
 * \~chinese
 * TelecomHelper 辅助类，用于处理来电弹出系统来电界面相关逻辑
 *
 * \~english
 * TelecomHelper helper class,used to handle incoming call related logic
 */
object TelecomHelper {
    /**
     * \~chinese
     * 立即启动来电的静态方法，无延时
     *
     * @param context 上下文
     * @param callerId 来电号码
     * @param callerName 来电者姓名
     *
     * \~english
     * Start call immediately, no delay
     * @param context context
     * @param callerId caller id
     * @param callerName caller name
     */
    fun startCallImmediately(
        context: Context,
        callerId: String = "calling",
        callerName: String = "CallKit Call"
    ) {
        ChatLog.d("TelecomHelper", "Starting call immediately - Caller: $callerName ($callerId)")

        try {
            // 通过IncomingCallService处理来电
            IncomingCallService.startService(context, callerId, callerName)
        } catch (e: Exception) {
            ChatLog.e("TelecomHelper", "Failed to start service: ${e.message}")
            // 如果启动服务失败，尝试直接处理来电
            handleIncomingCallDirectly(context, callerId, callerName)
        }
    }
    /**
     * \~chinese
     * 停止来电服务
     *
     * \~english
     * Stop incoming call service
     */
    fun stopService(context: Context) {
        try {
            IncomingCallService.stopService(context)
            ChatLog.d("TelecomHelper", "Service stopped successfully")
        } catch (e: Exception) {
            ChatLog.e("TelecomHelper", "Failed to stop service: ${e.message}")
        }
    }


    /**
     * \~chinese
     * 直接处理来电的备用方案,启动callkit默认来电页面
     *
     * \~english
     * Directly handle incoming call as a fallback, start the default incoming call page of callkit
     */
    private fun handleIncomingCallDirectly(context: Context, callerId: String, callerName: String) {
        CallKitClient.signalingManager.startSendEvent()
    }

    /**
     * \~chinese
     * 启动来电，支持延时
     *
     * @param context 上下文
     * @param callerId 来电号码，
     * @param callerName 来电者姓名
     * @param delaySeconds 延时秒数，默认为 4 秒
     *
     * \~english
     * Start incoming call, support delay
     * @param context context
     * @param callerId caller id
     * @param callerName caller name
     * @param delaySeconds delay seconds, default is 4 seconds
     */
    fun startCall(
        context: Context,
        callerId: String = "calling",
        callerName: String = "CallKit Call",
        delaySeconds: Int = 4
    ) {
        val delayMillis = delaySeconds * 1000L
        Toast.makeText(
            context,
            "startCall called, will trigger incoming call in $delaySeconds seconds...",
            Toast.LENGTH_SHORT
        ).show()

        // 延时后触发来电
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(
                context,
                "$delaySeconds seconds delay completed, triggering incoming call now",
                Toast.LENGTH_SHORT
            ).show()
            startCallImmediately(context, callerId, callerName)
        }, delayMillis)
    }


} 