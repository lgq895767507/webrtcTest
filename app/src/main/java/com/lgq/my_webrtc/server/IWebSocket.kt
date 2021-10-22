package com.lgq.my_webrtc.server

import org.webrtc.IceCandidate

interface IWebSocket {

    fun connect(wss: String?)

    fun isOpen(): Boolean

    fun close()

    // 加入房间
    fun joinRoom(room: String?)

    //处理回调消息
    fun handleMessage(message: String?)

    fun sendIceCandidate(socketId: String?, iceCandidate: IceCandidate?)

    fun sendAnswer(socketId: String?, sdp: String?)

    fun sendOffer(socketId: String?, sdp: String?)
}
