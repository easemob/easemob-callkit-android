package com.hyphenate.callkit.interfaces

import com.hyphenate.chat.EMRTCTokenInfo
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * \~chinese
 * 用户自定义Agora RTC配置，包含提供Agora AppId、Agora Rtc token等
 *
 * \~english
 * User-defined Agora RTC configuration, including providing Agora AppId, Agora Rtc token, etc.
 */
interface RTCConfigProvider {

    /**
     * \~chinese
     * 同步提供Agora AppId
     *
     * \~english
     * Sync provide Agora AppId
     */
    fun onSyncGetAppId(): String

    /**
     * \~chinese
     * 异步提供 Agora Rtc token
     * @param channelName 频道名称
     * @param callback 回调
     *
     * \~english
     * Async provide Agora Rtc token
     * @param channelName channel name
     * @param callback callback
     */
    fun onAsyncFetchRtcToken(channelName:String?,callback: OnValueSuccess<EMRTCTokenInfo>)
}

/**
 * \~chinese
 * 挂起函数 获取Agora Rtc token
 * @param channelName 频道名称
 * @return Agora Rtc token
 *
 * \~english
 * Suspend function to get Agora Rtc token
 * @param channelName channel name
 * @return Agora Rtc token
 */
suspend fun RTCConfigProvider.getRtcToken(channelName: String?): EMRTCTokenInfo {
    return suspendCoroutine { continuation ->
        onAsyncFetchRtcToken(channelName,callback = { info ->
            continuation.resume(info)
        })
    }
}
