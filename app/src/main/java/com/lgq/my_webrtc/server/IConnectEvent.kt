package com.lgq.my_webrtc.server

interface IConnectEvent {

    fun onSuccess()

    fun onFailed(msg: String?)
}
