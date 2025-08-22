package com.hyphenate.callkit.repo

import com.hyphenate.callkit.CallKitClient
import com.hyphenate.callkit.bean.CallKitUserInfo
import com.hyphenate.callkit.interfaces.getSyncUser
import com.hyphenate.callkit.utils.ChatClient
import com.hyphenate.callkit.utils.ChatGroupManager
import com.hyphenate.callkit.utils.ChatLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatCallKitGroupRepository(
    private val groupManager: ChatGroupManager = ChatClient.getInstance().groupManager()
) {
    private var Max: Int = 1000


    companion object {
        private const val TAG = "GroupRep"
        private const val LIMIT = 50
    }

    suspend fun loadLocalMember(groupId: String): MutableList<CallKitUserInfo> =
        withContext(Dispatchers.IO) {
            val data = mutableListOf<CallKitUserInfo>()
            val currentGroup = ChatClient.getInstance().groupManager().getGroup(groupId)
            val members = mutableListOf<String>()
            currentGroup?.members?.forEach {
                members.add( it)
            }
            currentGroup?.owner?.let {
                members.add(it)
            }
            currentGroup?.adminList?.forEach {
                members.add( it)
            }
            members.let {
                for (userId in it) {
                    CallKitClient.callInfoProvider?.getSyncUser(userId)?.let { info ->
                        data.add(info)
                    } ?: run {
                        data.add(CallKitUserInfo(userId=userId))
                    }
                }
            }
            data.toMutableList()
        }

    @Throws(Exception::class)
    suspend fun fetGroupMemberFromServer(
        groupId: String
    ): MutableList<CallKitUserInfo> = withContext(Dispatchers.IO) {
        try {
            val groupMemberList = mutableListOf<CallKitUserInfo>()
            var cursor: String? = null
            do {
                val result = groupManager.fetchChatGroupMembers(groupId, cursor, LIMIT)
                val data = CallKitClient.getCache().getUserInfosByIds(result.data)
                cursor = result.cursor
                groupMemberList.addAll(data)
            } while (!cursor.isNullOrEmpty() && groupMemberList.size <= Max)
            //添加管理员和成员
            val group= groupManager.getGroup(groupId)?:groupManager.fetchChatGroup(groupId)
            group.owner?.let{
                val ownerInfo = CallKitClient.getCache().getUserInfoById(it)
                groupMemberList.add(ownerInfo)
            }
            group.adminList?.let {
                val infos = CallKitClient.getCache().getUserInfosByIds(it)
                groupMemberList.addAll(infos)
            }
            groupMemberList
        } catch (e: Exception) {
            ChatLog.e(TAG, "Unexpected error while fetching group members: ${e.message}, fallback to local data")
            // 其他异常也回退到本地数据
            return@withContext loadLocalMember(groupId)
        }

    }
}