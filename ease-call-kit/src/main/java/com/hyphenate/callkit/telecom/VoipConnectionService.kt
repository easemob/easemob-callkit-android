package com.hyphenate.callkit.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.hyphenate.callkit.CallKitClient
import com.hyphenate.callkit.telecom.IncomingCallService.Companion.EXTRA_CALLER_DISPLAY_NAME_COMPAT
import com.hyphenate.callkit.utils.ChatLog
import java.util.UUID

/**
 * \~chinese
 * VoipConnectionService
 * 用于处理VoIP通话连接的Service
 *
 * \~english
 * VoipConnectionService
 * Service for handling VoIP call connections
 */
class VoipConnectionService : ConnectionService() {

    private val TAG = "VoipConnectionService"
    private val activeConnections = mutableMapOf<String, Connection>()
    private var callActionReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        registerCallActionReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterCallActionReceiver()
    }

    private fun registerCallActionReceiver() {
        callActionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val callId = intent?.getStringExtra("call_id") ?: return
                val connection = activeConnections[callId] ?: return

                when (intent.action) {
                    "ACTION_ANSWER_CALL" -> {
                        ChatLog.d(TAG, "Received answer action for call: $callId")
                        connection.onAnswer()
                    }

                    "ACTION_REJECT_CALL" -> {
                        ChatLog.d(TAG, "Received reject action for call: $callId")
                        connection.onReject()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction("ACTION_ANSWER_CALL")
            addAction("ACTION_REJECT_CALL")
        }

        // 使用 ContextCompat.registerReceiver 以支持所有Android版本
        ContextCompat.registerReceiver(
            this,
            callActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterCallActionReceiver() {
        callActionReceiver?.let {
            unregisterReceiver(it)
            callActionReceiver = null
        }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        // 获取来电信息
        val callerId = request.address?.schemeSpecificPart ?: "未知号码"
        val callerName = request.extras.getString(EXTRA_CALLER_DISPLAY_NAME_COMPAT)
            ?: request.extras.getString(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)
            ?: callerId
        val callId = request.extras.getString("call_id") ?: UUID.randomUUID().toString()
        ChatLog.d(TAG, "Incoming call from: $callerName ($callerId), Call ID: $callId")

        // 创建通话连接
        val connection = createConnection(callerId, callerName, callId)
        activeConnections[callId] = connection

        return connection.apply {
            setRinging() // 设置状态为响铃
        }
    }

    private fun createConnection(callerId: String, callerName: String, callId: String): Connection {
        return object : Connection() {
            init {
                setAddress(Uri.parse("tel:$callerId"), TelecomManager.PRESENTATION_ALLOWED)
                setCallerDisplayName(callerName, TelecomManager.PRESENTATION_ALLOWED)
                connectionCapabilities = CAPABILITY_HOLD or
                        CAPABILITY_SUPPORT_HOLD or
                        CAPABILITY_MUTE

                // 移除 SELF_MANAGED 属性，使用标准模式
                // connectionProperties = PROPERTY_SELF_MANAGED
                audioModeIsVoip = true

                // 设置通话额外信息
                val extras = Bundle().apply {
                    putString("call_id", callId)
                }
                putExtras(extras)
            }

            override fun onAnswer() {
                super.onAnswer()
                // 这里可以启动通话界面
                startCallActivity()
                setActive() // 接听电话
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
                activeConnections.remove(callId)
                ChatLog.d(TAG, "Call answered: $callId")
            }

            override fun onReject() {
                super.onReject()
                setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                destroy()
                activeConnections.remove(callId)
                // 通知服务器拒绝来电
                notifyCallRejected(callId)
            }

            override fun onDisconnect() {
                super.onDisconnect()
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
                activeConnections.remove(callId)
                // 通知服务器挂断电话
                notifyCallEnded(callId)
            }

            override fun onShowIncomingCallUi() {
                super.onShowIncomingCallUi()
                ChatLog.d("VoipConnectionService", "Showing incoming call UI for callId: $callId")

                // 对于非Self-Managed模式，系统会自动显示来电界面
                // 不需要手动启动自定义界面
            }

            override fun onStateChanged(state: Int) {
                super.onStateChanged(state)
                ChatLog.d(TAG, "Connection state changed to: $state for callId: $callId")
            }

            private fun startCallActivity() {
                ChatLog.d(TAG, "Starting call activity for callId: $callId")
                // 发送接听消息给对方
                CallKitClient.signalingManager.answerCall()
                // 这里可以启动通话中的界面
                CallKitClient.signalingManager.startSendEvent()
            }

            private fun notifyCallRejected(callId: String) {
                // 实现通知服务器
                ChatLog.d(TAG, "Call rejected: $callId")
                CallKitClient.signalingManager.refuseCall()
            }

            private fun notifyCallEnded(callId: String) {
                // 实现通知服务器
                ChatLog.d(TAG, "Call ended: $callId")
            }
        }
    }
}