package com.hyphenate.callkit.interfaces

import com.hyphenate.callkit.bean.CallKitUserInfo
import kotlinx.coroutines.flow.Flow

/**
 * \~chinese
 * 群组请求接口
 *
 * \~english
 * Group request interface
 */
interface IGroupRequest  {
    /**
     * \~chinese
     * 从服务获取群成员
     * @param groupId 群组ID
     *
     * \~english
     * Get Group Member
     * @param groupId group ID
     */
    suspend fun fetchGroupMemberFromService(groupId:String): Flow<MutableList<CallKitUserInfo>>

    /**
     * \~chinese
     * 加载本地成员
     * @param groupId 群组ID
     *
     * \~english
     * Load local member
     * @param groupId group ID
     */
    suspend fun loadLocalMember(groupId:String): Flow<MutableList<CallKitUserInfo>>

}