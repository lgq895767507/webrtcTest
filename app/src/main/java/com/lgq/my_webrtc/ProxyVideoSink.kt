package com.lgq.my_webrtc

import org.webrtc.VideoFrame
import org.webrtc.VideoSink

/**
 * @author : liuguoqing
 * time   : 2021/10/22
 * desc   :
 */
class ProxyVideoSink : VideoSink {

    private var target: VideoSink? = null

    override fun onFrame(frame: VideoFrame?) {
        target?.onFrame(frame)
    }

    @Synchronized
    fun setTarget(target: VideoSink?) {
        this.target = target
    }
}