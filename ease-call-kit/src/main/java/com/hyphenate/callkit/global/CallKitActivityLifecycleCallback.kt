package com.hyphenate.callkit.global

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Log
import com.hyphenate.callkit.CallKitClient
import com.hyphenate.callkit.bean.CallState
import com.hyphenate.callkit.manager.FloatWindow
import com.hyphenate.callkit.ui.MultiCallActivity
import com.hyphenate.callkit.ui.SingleCallActivity
import com.hyphenate.callkit.utils.ChatLog
import com.hyphenate.push.platform.normal.a.f

class CallKitActivityLifecycleCallback: Application.ActivityLifecycleCallbacks{
    private val TAG = "CallKit ActivityLifecycle"
    private val resumeActivity = mutableListOf<Activity>()
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        moveCallKitActivityToFront(activity)
    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {
    }


    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    private fun moveCallKitActivityToFront(activity: Activity) {
        if (!CallKitClient.floatWindow.isFloatWindowShowing()
            && CallKitClient.callState.value == CallState.CALL_ANSWERED
            && (activity !is SingleCallActivity)
            && (activity !is MultiCallActivity)){
            ChatLog.d(TAG, "moveCallKitActivityToFront: move to front")
            CallKitClient.signalingManager.startSendEvent(0)
        }
    }
}