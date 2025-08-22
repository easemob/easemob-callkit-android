package com.hyphenate.callkit.demo

import android.R.attr.password
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hyphenate.callkit.CallKitClient
import com.hyphenate.callkit.CallKitConfig
import com.hyphenate.callkit.bean.CallType
import com.hyphenate.callkit.demo.databinding.ActivityMainBinding
import com.hyphenate.callkit.utils.ChatClient
import com.hyphenate.chat.EMClient
import com.hyphenate.callkit.bean.CallEndReason
import com.hyphenate.callkit.bean.CallInfo
import com.hyphenate.callkit.interfaces.CallKitListener
import com.hyphenate.callkit.utils.ChatCallback
import com.hyphenate.callkit.utils.ChatOptions
import com.hyphenate.callkit.utils.ChatConnectionListener
import com.hyphenate.callkit.utils.ChatLog
import io.agora.rtc2.RtcEngine
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var isLoggedIn = false
    private val TAG = this::class.simpleName
    private val selfUserID="" // 替换为你登录用的用户ID
    private val remoteUserID="" // 替换为实际远端用户ID，用于单聊通话
    private val imToken="" // 替换为IM 登录Token
    private val groupID="" // 替换为实际群组ID
    private val imAppkey="" // 替换为实际环信AppKey
    

    private val rtcListener: CallKitListener = object : CallKitListener {

        override fun onEndCallWithReason(reason: CallEndReason, callInfo: CallInfo?) {
            runOnUiThread {
                val msg = "通话结束: $reason ,callInfo: $callInfo"
                ChatLog.d(TAG, msg)
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onCallError(
            errorType: CallKitClient.CallErrorType,
            errorCode: Int,
            description: String?
        ) {
            runOnUiThread {
                val msg = "通话错误: $errorType ,errorCode: $errorCode ,description: $description"
                ChatLog.d(TAG, msg)
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onReceivedCall(userId: String, callType: CallType, ext: JSONObject?) {
            runOnUiThread {
                val msg = "收到通话邀请: $userId ,callType: $callType ,ext: $ext"
                ChatLog.d(TAG, msg)
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onRemoteUserJoined(userId: String, callType: CallType, channelName: String) {
            runOnUiThread {
                val msg = "远端用户加入: $userId ,callType: $callType ,channelName: $channelName"
                ChatLog.d(TAG, msg)
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onRemoteUserLeft(userId: String, callType: CallType, channelName: String) {
            runOnUiThread {
                val msg = "远端用户离开: $userId ,callType: $callType ,channelName: $channelName"
                ChatLog.d(TAG, msg)
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onRtcEngineCreated(engine: RtcEngine) {
            runOnUiThread {
                val msg = "RTC引擎创建: $engine"
                ChatLog.d(TAG, msg)
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

    }
    
    private val connectionListener = object : ChatConnectionListener {
        override fun onConnected() {
            runOnUiThread {
                updateConnectionStatus(true, "连接状态: 已连接")
            }
        }
        
        override fun onDisconnected(errorCode: Int) {
            runOnUiThread {
                updateConnectionStatus(false, "连接状态: 已断开 (错误码: $errorCode)")
            }
        }
        
        override fun onLogout(errorCode: Int) {
            runOnUiThread {
                updateConnectionStatus(false, "连接状态: 已登出 (错误码: $errorCode)")
                isLoggedIn = false
                updateButtonStates()
            }
        }
        
        override fun onTokenExpired() {
            runOnUiThread {
                updateConnectionStatus(false, "连接状态: Token已过期")
                Toast.makeText(this@MainActivity, "Token已过期，请重新登录", Toast.LENGTH_LONG).show()
            }
        }
        
        override fun onTokenWillExpire() {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Token即将过期", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initCallKit()
        setupClickListeners()
        updateButtonStates()
        setupConnectionListener()
    }
    
    private fun initCallKit() {
        // 初始化环信IM SDK
        val options = ChatOptions().apply {
            appKey = imAppkey
            autoLogin = false
        }
        ChatClient.getInstance().init(this, options)
        ChatClient.getInstance().setDebugMode(true)
        
        // 初始化CallKit
        val config = CallKitConfig().apply {

            // 铃声文件配置示例：
            // 使用assets文件夹中的文件：
            outgoingRingFile = "assets://outgoing_ring.mp3"
            incomingRingFile = "assets://incoming_ring.mp3"
            dingRingFile = "assets://ding.mp3"

            // 使用res/raw文件夹中的music.mp3作为铃声
            // outgoingRingFile = "raw://outgoing_ring.mp3"

            // 使用绝对路径：
            // outgoingRingFile = "/storage/emulated/0/Download/incoming_ring.mp3"
        }

        CallKitClient.init(this, config)
        CallKitClient.callKitListener=rtcListener
    }
    
    private fun setupConnectionListener() {
        // 添加连接状态监听器
        ChatClient.getInstance().addConnectionListener(connectionListener)
        
        // 初始化连接状态显示
        updateConnectionStatus(false, "连接状态: 未连接")
    }

    private fun updateConnectionStatus(isConnected: Boolean, statusText: String) {
        binding.tvConnectionStatus.text = statusText
        ChatLog.e(TAG, "updateConnectionStatus: $statusText"+",isConnected:$isConnected")
        if (isConnected) {
            // 连接成功 - 绿色背景
            binding.statusIndicator.setBackgroundColor(0xFF4CAF50.toInt()) // 绿色
            binding.tvConnectionStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            // 连接失败或断开 - 灰色背景
            binding.statusIndicator.setBackgroundColor(0xFF808080.toInt()) // 灰色
            binding.tvConnectionStatus.setTextColor(0xFF808080.toInt())
        }
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            if (selfUserID.isEmpty()) {
                Toast.makeText(this, "用户名为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            login(selfUserID)
        }
        
        binding.btnLogout.setOnClickListener {
            logout()
        }
        
        binding.btnSingleVideo.setOnClickListener {
            startSingleVideoCall()
        }
        
        binding.btnSingleAudio.setOnClickListener {
            startSingleAudioCall()
        }
        
        binding.btnMultipleVideo.setOnClickListener {
            startMultipleVideoCall()
        }
    }
    
    private fun login(selfUserID: String) {
        if (isLoggedIn) {
            Toast.makeText(this, "已经登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 这里使用简单的密码，实际项目中应该使用更安全的认证方式

        EMClient.getInstance().loginWithToken(selfUserID, imToken, object : ChatCallback {
            override fun onSuccess() {
                runOnUiThread {
                    isLoggedIn = true
                    updateButtonStates()
                    Toast.makeText(this@MainActivity, "登录成功", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onError(code: Int, error: String?) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "登录失败: $error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
    
    private fun logout() {
        if (!isLoggedIn) {
            Toast.makeText(this, "尚未登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        EMClient.getInstance().logout(true, object : ChatCallback {
            override fun onSuccess() {
                runOnUiThread {
                    updateConnectionStatus(false, "连接状态: 已登出 ")
                    isLoggedIn = false
                    updateButtonStates()
                    CallKitClient.endCall()
                    Toast.makeText(this@MainActivity, "登出成功", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onError(code: Int, error: String?) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "登出失败: $error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
    
    private fun startSingleVideoCall() {
        if (!isLoggedIn) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        if (remoteUserID.isEmpty()) {
            Toast.makeText(this, "远端用户ID为空", Toast.LENGTH_SHORT).show()
            return
        }
        
        CallKitClient.startSingleCall(CallType.SINGLE_VIDEO_CALL, remoteUserID, null)
    }
    
    private fun startSingleAudioCall() {
        if (!isLoggedIn) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (remoteUserID.isEmpty()) {
            Toast.makeText(this, "远端用户ID为空", Toast.LENGTH_SHORT).show()
            return
        }
        
        CallKitClient.startSingleCall(CallType.SINGLE_VOICE_CALL, remoteUserID, null)
    }
    
    private fun startMultipleVideoCall() {
        if (!isLoggedIn) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }

        CallKitClient.startGroupCall(groupID, null)
    }

    private fun updateButtonStates() {
        binding.btnLogin.isEnabled = !isLoggedIn
//        binding.btnLogout.isEnabled = isLoggedIn
        binding.btnSingleVideo.isEnabled = isLoggedIn
        binding.btnSingleAudio.isEnabled = isLoggedIn
        binding.btnMultipleVideo.isEnabled = isLoggedIn
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 移除连接状态监听器
        ChatClient.getInstance().removeConnectionListener(connectionListener)
    }
}