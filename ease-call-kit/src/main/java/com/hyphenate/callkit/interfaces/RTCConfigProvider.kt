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
     * 默认返回null
     *
     * \~english
     * Sync provide Agora AppId
     * Default returns empty string
     */
    fun onSyncGetAppId(): String? = null

    /**
     * \~chinese
     * 异步提供 Agora Rtc token
     * 默认返回 null
     * @param channelName 频道名称
     * @param callback 回调
     *
     * \~english
     * Async provide Agora Rtc token
     * Default returns null
     * @param channelName channel name
     * @param callback callback
     */
    fun onAsyncFetchRtcToken(channelName:String?,callback: OnValueSuccess<EMRTCTokenInfo?>) = callback(null)

    /**
     * \~chinese
     * 异步通过 Agora uid 批量获取环信用户 id
     * 默认返回 null；返回 null 或映射中缺少对应 uid 时，将回退到 SDK 查询
     * @param uids Agora uid 列表
     * @param callback 回调，值为 uid -> 环信用户 id 的映射
     *
     * \~english
     * Async batch fetch Easemob user ids by Agora uids
     * Default returns null; falls back to SDK query when null or uid missing in the map
     * @param uids list of Agora uids
     * @param callback callback with a uid -> Easemob user id map
     */
    fun onAsyncFetchUserIdByUid(uids: List<Int>, callback: OnValueSuccess<Map<Int, String>?>) = callback(null)
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
suspend fun RTCConfigProvider.getRtcToken(channelName: String?): EMRTCTokenInfo? {
    return suspendCoroutine { continuation ->
        onAsyncFetchRtcToken(channelName,callback = { info ->
            continuation.resume(info)
        })
    }
}

/**
 * \~chinese
 * 挂起函数 通过 Agora uid 批量获取环信用户 id
 * @param uids Agora uid 列表
 * @return uid -> 环信用户 id 的映射
 *
 * \~english
 * Suspend function to batch fetch Easemob user ids by Agora uids
 * @param uids list of Agora uids
 * @return uid -> Easemob user id map
 */
suspend fun RTCConfigProvider.getUserIdsByUids(uids: List<Int>): Map<Int, String>? {
    return suspendCoroutine { continuation ->
        onAsyncFetchUserIdByUid(uids, callback = { map ->
            continuation.resume(map)
        })
    }
}
