package com.lgq.my_webrtc.server

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.lgq.my_webrtc.IViewCallback
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import java.util.*


/**
 * @author : liuguoqing
 * time   : 2021/10/22
 * desc   :
 */
class WebRTCManager : ISignalingEvents {

    private var mWss: String? = null
    private var mEvent: IConnectEvent? = null
    private var mWebSocket: IWebSocket? = null
    private var mRoomId: String? = null
    private var mMediaType = 0
    private var mVideoEnable = false
    private var mPeerHelper: PeerConnectionManager? = null
    private val mHandler = Handler(Looper.getMainLooper())

    companion object {
        fun getInstance(): WebRTCManager {
            return WebRTCManager.Holder.wrManager
        }
    }

    private object Holder {
        val wrManager = WebRTCManager()
    }

    /**
     * 初始化
     */
    fun init(wss: String, event: IConnectEvent) {
        this.mWss = wss
        this.mEvent = event
    }

    /**
     * 连接
     */
    fun connect(mediaType: Int, roomId: String) {
        if (mWebSocket == null) {
            mMediaType = mediaType
            mVideoEnable = mediaType != MediaType.TYPE_AUDIO
            mRoomId = roomId
            mWebSocket = JavaWebSocket(this)
            mWebSocket?.connect(mWss)
            mPeerHelper = PeerConnectionManager(mWebSocket, null)
        } else {
            mWebSocket?.close()
            mWebSocket = null
            mPeerHelper = null
        }
    }

    fun setCallback(callback: IViewCallback) {
        mPeerHelper?.setViewCallback(callback)
    }

    /**
     * 加入房间
     */
    fun joinRoom(context: Context, eglBase: EglBase) {
        mPeerHelper?.initContext(context, eglBase)
        mWebSocket?.joinRoom(mRoomId)
    }

    /**
     * 切换摄像头
     */
    fun switchCamera() {
        mPeerHelper?.switchCamera()
    }

    /**
     * 静音
     */
    fun toggleMute(enable: Boolean) {
        mPeerHelper?.toggleMute(enable)
    }

    /**
     * 禁扬声器
     */
    fun toggleSpeaker(enable: Boolean) {
        mPeerHelper?.toggleSpeaker(enable)
    }

    /**
     * 退出房间
     */
    fun exitRoom() {
        mWebSocket = null
        mPeerHelper?.exitRoom()
    }


    override fun onWebSocketOpen() {
        mHandler.post {
            mEvent?.onSuccess()
        }
    }

    override fun onWebSocketOpenFailed(msg: String?) {
        mHandler.post {
            if (mWebSocket != null && mWebSocket?.isOpen() == false) {
                mEvent?.onFailed(msg)
            } else {
                if (mPeerHelper != null) {
                    mPeerHelper?.exitRoom()
                }
            }
        }
    }

    override fun onJoinToRoom(connections: ArrayList<String?>?, myId: String?) {
        mHandler.post {
            mPeerHelper?.onJoinToRoom(connections, myId, mVideoEnable, mMediaType)
            if (mMediaType == MediaType.TYPE_VIDEO || mMediaType == MediaType.TYPE_MEETING) {
                toggleSpeaker(true)
            }
        }
    }

    override fun onRemoteJoinToRoom(socketId: String?) {
        mHandler.post {
            mPeerHelper?.onRemoteJoinToRoom(socketId)
        }
    }

    override fun onRemoteIceCandidate(socketId: String?, iceCandidate: IceCandidate?) {
        mHandler.post {
            mPeerHelper?.onRemoteIceCandidate(socketId, iceCandidate)
        }
    }

    override fun onRemoteIceCandidateRemove(
        socketId: String?,
        iceCandidates: List<IceCandidate?>?
    ) {
        mHandler.post {
            mPeerHelper?.onRemoteIceCandidateRemove(socketId, iceCandidates)
        }
    }

    override fun onRemoteOutRoom(socketId: String?) {
        mHandler.post {
            mPeerHelper?.onRemoteOutRoom(socketId)
        }
    }

    override fun onReceiveOffer(socketId: String?, sdp: String?) {
        mHandler.post {
            mPeerHelper?.onReceiveOffer(socketId, sdp)
        }
    }

    override fun onReceiverAnswer(socketId: String?, sdp: String?) {
        mHandler.post {
            mPeerHelper?.onReceiverAnswer(socketId, sdp)
        }
    }


}