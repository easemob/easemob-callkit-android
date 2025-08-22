package com.hyphenate.callkit.manager

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import com.hyphenate.callkit.CallKitClient
import com.hyphenate.callkit.utils.ChatLog
import java.io.IOException


/**
 * \~chinese
 * 音频控制管理器
 * 负责音频播放、铃声管理
 *
 * \~english
 * Audio controller
 * Responsible for audio playback, ringtone management
 */
class AudioController {

    internal enum class RingType {
        OUTGOING,  // 外呼铃声
        INCOMING,  // 来电铃声
        DING    // 结束警告铃声 ding
    }

    private  val TAG = "CallKitAudioController"

    private var ringtone: Ringtone? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mContext: Context
    private lateinit var audioManager: AudioManager
    private var isPlayDing = false


    /**
     * 初始化音频控制器
     */
    internal fun init(context: Context) {
        this.mContext = context.applicationContext
        audioManager = mContext.getSystemService(AUDIO_SERVICE) as AudioManager
        // 开始振铃设置
        val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        audioManager.setMode(AudioManager.MODE_RINGTONE)
        if (ringUri != null) {
            ringtone = RingtoneManager.getRingtone(mContext, ringUri)
        }
        mediaPlayer=MediaPlayer()
    }

    /**
     * 播放铃声
     */
    @Synchronized
    internal fun playRing(ringType: RingType?=null) {
        val ringerMode: Int = audioManager.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            ChatLog.e(TAG, "playRing start ringtone, ringType: $ringType")
            val ringFile: String? = when(ringType){
                RingType.OUTGOING -> CallKitClient.callKitConfig.outgoingRingFile
                RingType.INCOMING -> CallKitClient.callKitConfig.incomingRingFile
                RingType.DING -> CallKitClient.callKitConfig.dingRingFile
                else -> null
            }
            if (ringFile != null) {
                isPlayDing = if (ringType == RingType.DING) true else false
                // 确保 MediaPlayer 处于正确状态
                try {
                    mediaPlayer?.reset()
                } catch (e: Exception) {
                    ChatLog.e(TAG, "Error resetting MediaPlayer: ${e.message}")
                }
                mediaPlayer = MediaPlayer()
                try {
                    mediaPlayer?.apply {
                        when {
                            ringFile.startsWith("assets://") -> {
                                // 处理assets文件
                                val assetFileName = ringFile.substring(9) // 移除 "assets://" 前缀
                                val assetFileDescriptor = mContext.assets.openFd(assetFileName)
                                setDataSource(
                                    assetFileDescriptor.fileDescriptor,
                                    assetFileDescriptor.startOffset,
                                    assetFileDescriptor.length
                                )
                                assetFileDescriptor.close()
                            }

                            ringFile.startsWith("raw://") -> {
                                // 处理res/raw资源文件
                                val resourceName = ringFile.substring(6) // 移除 "raw://" 前缀
                                val resourceId = mContext.resources.getIdentifier(
                                    resourceName.substringBeforeLast('.'), // 移除文件扩展名
                                    "raw",
                                    mContext.packageName
                                )
                                if (resourceId != 0) {
                                    val rawFileDescriptor =
                                        mContext.resources.openRawResourceFd(resourceId)
                                    setDataSource(
                                        rawFileDescriptor.fileDescriptor,
                                        rawFileDescriptor.startOffset,
                                        rawFileDescriptor.length
                                    )
                                    rawFileDescriptor.close()
                                } else {
                                    throw IOException("Raw resource not found: $resourceName")
                                }
                            }

                            else -> {
                                // 处理普通文件路径
                                setDataSource(ringFile)
                            }
                        }

                        setOnCompletionListener {
                            ChatLog.d(TAG, "playRing completed: ${ringFile}")
                            //除ding铃声外，其他铃声播放完毕后自动循环
                           if (ringType!= RingType.DING ){
                               start()
                           } else {
                               // DING 播放完成，可以释放资源
                               isPlayDing = false
                               releaseMediaPlayer()
                           }
                        }
                        setOnErrorListener { mp, what, extra ->
                            isPlayDing=false
                            ChatLog.e(TAG, "playRing error: what=$what, extra=$extra")
                            true
                        }
                        if (!isPlaying) {
                            prepare()
                            start()
                            ChatLog.e(TAG, "playRing play file: $ringFile")
                        }
                    }
                } catch (e: Exception) {
                    releaseMediaPlayer()
                    ChatLog.e(TAG, "playRing error: ${e.message}")
                    // 如果自定义铃声播放失败，回退到系统铃声
                    if (ringType!= RingType.DING){
                        ringtone?.play()
                    }
                }
            } else {
                if (ringType!= RingType.DING){
                    ringtone?.play()
                }
                ChatLog.e(TAG, "playRing play ringtone")
            }
        }
    }

    @Synchronized
    internal fun stopPlayRingAndPlayDing(){
        stopPlayRing()
        playRing(RingType.DING)
    }

    /**
     * 停止播放铃声
     */
    @Synchronized
    internal fun stopPlayRing() {
        ChatLog.d(TAG, "stopPlayRing")
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
        }
        ringtone?.stop()
    }

    /**
     * 释放资源
     */
    internal fun exitCall() {
        ChatLog.d(TAG, "exitCall")
        //播放ding的时候让ding播放完后自己去处理释放
        if (!isPlayDing) {
            releaseMediaPlayer()
        }
    }
    
    private fun releaseMediaPlayer() {
        ChatLog.d(TAG, "releaseMediaPlayer")
        stopPlayRing()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}