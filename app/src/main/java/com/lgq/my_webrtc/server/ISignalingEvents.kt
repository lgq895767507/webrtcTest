package com.lgq.my_webrtc.server

import org.webrtc.IceCandidate
import java.util.ArrayList

interface ISignalingEvents {
    // webSocket连接成功
    fun onWebSocketOpen()

    // webSocket连接失败
    fun onWebSocketOpenFailed(msg: String?)

    // 进入房间
    fun onJoinToRoom(connections: ArrayList<String?>?, myId: String?)

    // 有新人进入房间
    fun onRemoteJoinToRoom(socketId: String?)

    fun onRemoteIceCandidate(socketId: String?, iceCandidate: IceCandidate?)

    fun onRemoteIceCandidateRemove(socketId: String?, iceCandidates: List<IceCandidate?>?)


    fun onRemoteOutRoom(socketId: String?)

    fun onReceiveOffer(socketId: String?, sdp: String?)

    fun onReceiverAnswer(socketId: String?, sdp: String?)
}
