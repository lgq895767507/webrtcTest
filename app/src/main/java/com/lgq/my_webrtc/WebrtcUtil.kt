package com.lgq.my_webrtc

import android.app.Activity
import android.text.TextUtils
import android.util.Log
import com.lgq.my_webrtc.server.IConnectEvent
import com.lgq.my_webrtc.server.MediaType
import com.lgq.my_webrtc.server.WebRTCManager

/**
 * @author : liuguoqing
 * time   : 2021/10/22
 * desc   :
 */
object WebrtcUtil {

    private const val TAG = "WebrtcUtil"
    private const val WSS = "wss://1.117.248.121/wss"

    /**
     * 加入房间
     */
    fun call(activity: Activity, wss: String, roomId: String) {
        WebRTCManager.getInstance().init(wss, object : IConnectEvent {
            override fun onSuccess() {
                ChatRoomActivity.openActivity(activity)
            }

            override fun onFailed(msg: String?) {
                Log.e(TAG, msg ?: "init onFailed")
            }

        })
        WebRTCManager.getInstance().connect(MediaType.TYPE_MEETING, roomId)
    }

    /**
     * 一对一
     */
    fun callSingle(activity: Activity, wss: String, roomId: String, videoEnable: Boolean) {
        WebRTCManager.getInstance().init(wss, object : IConnectEvent {
            override fun onSuccess() {
                ChatSingleActivity.openActivity(activity, videoEnable)
            }

            override fun onFailed(msg: String?) {
                Log.e(TAG, msg ?: "init onFailed")
            }

        })
        WebRTCManager.getInstance()
            .connect(if (videoEnable) MediaType.TYPE_VIDEO else MediaType.TYPE_AUDIO, roomId)
    }

}