package com.hyphenate.callkit.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import com.hyphenate.callkit.R
import com.hyphenate.callkit.bean.CallKitUserInfo
import com.hyphenate.callkit.bean.NetworkQuality
import com.hyphenate.callkit.extension.dpToPx
import com.hyphenate.callkit.utils.CallKitUtils.setBgRadius
import com.hyphenate.callkit.utils.ChatLog

/**
 * \~chinese
 * 多人视频通话成员视图
 * 支持显示视频画面、用户头像、用户名、麦克风状态、网络状态等
 *
 * \~english
 * Multi-video call member view, supports displaying video view, user avatar, user name, microphone status, network status, etc.
 * After refactoring, directly use the status information in CallKitUserInfo, no longer maintain local status
 */
class MultiVideoCallMemberView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "MultiVideoCallMemberView"
        private const val BORDER_WIDTH = 3f
        private const val CORNER_RADIUS = 8f
    }

    // UI组件
    private lateinit var videoTexture: TextureView
    private lateinit var avatarImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var micStatusImageView: ImageView
    private lateinit var networkStatusImageView: ImageView
    private lateinit var speakingIndicator: View
    lateinit var cslConnecting: View
    private lateinit var llContainer: View

    // 用户信息 - 包含所有状态
    private var userInfo: CallKitUserInfo? = null
    
    // 绘制相关
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val speakingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    init {
        initView()
        initPaint()
    }

    private fun initView() {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.view_multi_video_call_member, this, true)
        
        // 初始化UI组件
        videoTexture = findViewById(R.id.texture_view)
        avatarImageView = findViewById(R.id.avatar_image)
        userNameTextView = findViewById(R.id.user_name)
        micStatusImageView = findViewById(R.id.mic_status)
        networkStatusImageView = findViewById(R.id.network_status)
        speakingIndicator = findViewById(R.id.speaking_indicator)
        cslConnecting = findViewById(R.id.csl_connecting)
        llContainer = findViewById(R.id.ll_container)

        setBgRadius( this, 8.dpToPx(context))

        // 设置默认状态
        updateUI()
        ChatLog.d(TAG, "initView() completed, all UI components initialized")
    }

    private fun initPaint() {
        borderPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = BORDER_WIDTH
            color = Color.TRANSPARENT
        }
        
        speakingPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = BORDER_WIDTH * 2
            color = ContextCompat.getColor(context, R.color.callkit_speaking_indicator_color)
        }
        ChatLog.d(TAG, "initPaint() completed, borderPaint and speakingPaint configured")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制说话状态边框
        val isSpeaking = userInfo?.isSpeaking ?: false
        if (isSpeaking) {
            rectF.set(
                speakingPaint.strokeWidth / 2,
                speakingPaint.strokeWidth / 2,
                width - speakingPaint.strokeWidth / 2,
                height - speakingPaint.strokeWidth / 2
            )
            canvas.drawRoundRect(rectF, CORNER_RADIUS, CORNER_RADIUS, speakingPaint)
            ChatLog.d(TAG, "onDraw() speaking border drawn")
        }
    }

    /**
     * 拦截触摸事件，防止子视图消费点击
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 在DOWN时开始拦截，后续事件都会交给本View处理
        return true
    }

    /**
     * 设置用户信息 - 这是主要的更新入口
     */
    fun setUserInfo(userInfo: CallKitUserInfo) {
        ChatLog.d(TAG, "setUserInfo() called, userId: ${userInfo.userId}, nickName: ${userInfo.nickName}")
        this.userInfo = userInfo
        // 更新UI
        updateUI()
    }

    private fun loadUserAvatar(userInfo: CallKitUserInfo) {
        if (!userInfo.avatar.isNullOrEmpty()) {
            ChatLog.d(TAG, "loadUserAvatar() loading avatar: ${userInfo.avatar}")
            avatarImageView.load(userInfo.avatar) {
                error(R.drawable.callkit_video_default)
                placeholder(R.drawable.callkit_video_default)
            }
        } else {
            ChatLog.d(TAG, "loadUserAvatar() using default avatar")
            avatarImageView.setImageResource(R.drawable.callkit_video_default)
        }
    }

    /**
     * 设置视频视图
     */
    fun setVideoView(block: (textureView: TextureView) -> Unit) {
        ChatLog.d(TAG, "setVideoView() called, textureView: $videoTexture"+
            ", videoEnabled: ${userInfo?.isVideoEnabled}")
        if (isVideoEnabled()){
            block(videoTexture)
        }
        updateUI()
    }

    /**
     * 兼容性方法 - 设置视频启用状态
     * 注意：这些方法只是为了兼容性，实际状态应该通过setUserInfo更新
     */
    fun setVideoEnabled(enabled: Boolean) {
        ChatLog.d(TAG, "setVideoEnabled() called, enabled: $enabled")
        userInfo?.let { info ->
            if (info.isVideoEnabled != enabled) {
                // 更新userInfo中的状态
                info.isVideoEnabled = enabled
                updateUI()
                ChatLog.d(TAG, "setVideoEnabled() video status changed to: $enabled")
            }
        }
    }

    /**
     * 兼容性方法 - 设置麦克风启用状态
     */
    fun setMicEnabled(enabled: Boolean) {
        ChatLog.d(TAG, "setMicEnabled() called, enabled: $enabled")
        userInfo?.let { info ->
            if (info.isMicEnabled != enabled) {
                info.isMicEnabled = enabled
                updateUI()
                ChatLog.d(TAG, "setMicEnabled() mic status changed to: $enabled")
            }
        }
    }

    /**
     * 兼容性方法 - 设置说话状态
     */
    fun setSpeaking(speaking: Boolean) {
        ChatLog.d(TAG, "setSpeaking() called, speaking: $speaking")
        userInfo?.let { info ->
            if (info.isSpeaking != speaking) {
                info.isSpeaking = speaking
                updateUI()
                invalidate() // 重绘边框
                ChatLog.d(TAG, "setSpeaking() speaking status changed to: $speaking")
            }
        }
    }

    /**
     * 兼容性方法 - 设置网络质量
     */
    fun setNetworkQuality(quality: NetworkQuality) {
        ChatLog.d(TAG, "setNetworkQuality() called, quality: $quality")
        userInfo?.let { info ->
            if (info.networkQuality != quality) {
                info.networkQuality = quality
                updateUI()
                ChatLog.d(TAG, "setNetworkQuality() network quality changed to: $quality")
            }
        }
    }

    /**
     * 兼容性方法 - 设置连接状态
     */
    fun setConnected(connected: Boolean) {
        ChatLog.d(TAG, "setConnected() called, connected: $connected")
        userInfo?.let { info ->
            if (info.connected != connected) {
                info.connected = connected
                updateUI()
                ChatLog.d(TAG, "setConnected() connection status changed to: $connected")
            }
        }
    }

    /**
     * 更新UI显示 - 基于userInfo中的状态
     */
    private fun updateUI() {
        val info = userInfo
        if (info == null) {
            Log.w(TAG, "updateUI() called but userInfo is null")
            return
        }

        ChatLog.d(TAG, "updateUI() called for user: ${info.userId}")
        
        // 更新视频显示
        if (info.isVideoEnabled) {
            videoTexture.visibility = VISIBLE
            avatarImageView.visibility = GONE
            ChatLog.d(TAG, "updateUI() showing video view")
        } else {
            videoTexture.visibility = GONE
            avatarImageView.visibility = VISIBLE
            avatarImageView.load(userInfo?.avatar){
                error(R.drawable.callkit_video_default)
                placeholder(R.drawable.callkit_video_default)
            }
            ChatLog.d(TAG, "updateUI() showing avatar view")
        }
        // 更新连接状态
        cslConnecting.visibility = if (info.connected) GONE else VISIBLE
        
        // 更新麦克风状态
        micStatusImageView.visibility = if (info.isMicEnabled) GONE else VISIBLE
        if (!info.isMicEnabled) {
            micStatusImageView.setImageResource(R.drawable.callkit_mic_off)
        }
        ChatLog.d(TAG, "updateUI() mic status visibility: ${if (info.isMicEnabled) "GONE" else "VISIBLE"}")
        
        // 更新网络状态
        when (info.networkQuality) {
            NetworkQuality.GOOD -> {
                networkStatusImageView.visibility = VISIBLE
                networkStatusImageView.setImageResource(R.drawable.callkit_network_good)
                ChatLog.d(TAG, "updateUI() network status: GOOD (visible)")
            }
            NetworkQuality.POOR -> {
                networkStatusImageView.visibility = VISIBLE
                networkStatusImageView.setImageResource(R.drawable.callkit_network_poor)
                ChatLog.d(TAG, "updateUI() network status: POOR (visible)")
            }
            NetworkQuality.WORSE -> {
                networkStatusImageView.visibility = VISIBLE
                networkStatusImageView.setImageResource(R.drawable.callkit_network_worse)
                ChatLog.d(TAG, "updateUI() network status: WORSE (visible)")
            }
            NetworkQuality.NONE -> {
                networkStatusImageView.visibility = VISIBLE
                networkStatusImageView.setImageResource(R.drawable.callkit_network_none)
                ChatLog.d(TAG, "updateUI() network status: NONE (visible)")
            }
            NetworkQuality.UNKNOWN -> {
                networkStatusImageView.visibility = View.GONE
                ChatLog.d(TAG, "updateUI() network status: UNKNOWN (GONE)")
            }
        }
        
        // 更新说话指示器
        speakingIndicator.visibility = if (info.isSpeaking) VISIBLE else GONE
        ChatLog.d(TAG, "updateUI() speaking indicator visibility: ${if (info.isSpeaking) "VISIBLE" else "GONE"}")
        
        // 更新用户名显示
        userNameTextView.text= info.getName()

        ChatLog.d(TAG, "updateUI() completed for user: ${info.userId}")
    }

    /**
     * 获取用户信息
     */
    fun getUserInfo(): CallKitUserInfo? {
        ChatLog.d(TAG, "getUserInfo() called, userInfo: ${userInfo?.userId}")
        return userInfo
    }

    /**
     * 是否启用视频
     */
    fun isVideoEnabled(): Boolean {
        val enabled = userInfo?.isVideoEnabled ?: true
        ChatLog.d(TAG, "isVideoEnabled() called, returning: $enabled")
        return enabled
    }

    /**
     * 是否启用麦克风
     */
    fun isMicEnabled(): Boolean {
        val enabled = userInfo?.isMicEnabled ?: true
        ChatLog.d(TAG, "isMicEnabled() called, returning: $enabled")
        return enabled
    }

    /**
     * 是否正在说话
     */
    fun isSpeaking(): Boolean {
        val speaking = userInfo?.isSpeaking ?: false
        ChatLog.d(TAG, "isSpeaking() called, returning: $speaking")
        return speaking
    }

    /**
     * 获取网络质量
     */
    fun getNetworkQuality(): NetworkQuality {
        val quality = userInfo?.networkQuality ?: NetworkQuality.GOOD
        ChatLog.d(TAG, "getNetworkQuality() called, returning: $quality")
        return quality
    }

    /**
     * 是否已连接
     */
    fun isConnected(): Boolean {
        val connected = userInfo?.connected ?: false
        ChatLog.d(TAG, "isConnected() called, returning: $connected")
        return connected
    }

}