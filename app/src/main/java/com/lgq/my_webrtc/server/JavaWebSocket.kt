package com.lgq.my_webrtc.server

import android.util.Log
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.serializer.SerializerFeature
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.webrtc.IceCandidate
import java.lang.Exception
import java.net.URI
import java.net.URISyntaxException
import java.security.SecureRandom
import java.util.ArrayList
import java.util.HashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

/**
 * @author : liuguoqing
 * time   : 2021/10/22
 * desc   :
 */
class JavaWebSocket(val events: ISignalingEvents) : IWebSocket {


    private var mWebSocketClient: WebSocketClient? = null

    // 是否连接成功过
    private var mIsOpen: Boolean = false

    override fun connect(wss: String?) {
        var uri: URI? = null
        try {
            uri = URI(wss)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
        if (mWebSocketClient == null) {
            mWebSocketClient = object : WebSocketClient(uri) {
                override fun onOpen(p0: ServerHandshake?) {
                    mIsOpen = true
                    events.onWebSocketOpen()
                }

                override fun onMessage(message: String?) {
                    mIsOpen = true
                    handleMessage(message)
                    Log.d(TAG, message ?: "message is null")
                }

                override fun onClose(p0: Int, reason: String?, p2: Boolean) {
                    Log.e(TAG, "onClose: $reason")
                    events.onWebSocketOpenFailed(reason)
                }

                override fun onError(p0: Exception?) {
                    Log.e(TAG, p0.toString())
                    events.onWebSocketOpenFailed(p0.toString())
                }
            }
        }
        if (wss?.startsWith("wss") == true) {
            try {
                val instance = SSLContext.getInstance("TLS")
                instance.init(null, arrayOf<TrustManager>(TrustManagerTest()), SecureRandom())
                val factory: SSLSocketFactory = instance.socketFactory
                mWebSocketClient?.socket = factory.createSocket()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mWebSocketClient?.connect()
        }
    }

    override fun isOpen(): Boolean {
        return mIsOpen
    }

    override fun close() {
        mWebSocketClient?.close()
    }

    override fun joinRoom(room: String?) {
        val map: MutableMap<String, Any> = HashMap()
        map["eventName"] = "__join"
        val childMap: MutableMap<String, String> = HashMap()
        childMap["room"] = room!!
        map["data"] = childMap
        val `object` = JSONObject(map)
        val jsonString = `object`.toString()
        Log.d(TAG, "send-->$jsonString")
        mWebSocketClient!!.send(jsonString)
    }

    override fun handleMessage(message: String?) {
        val map = JSON.parseObject<Map<*, *>>(
            message,
            MutableMap::class.java
        )
        val eventName = map["eventName"] as String?
        Log.i(TAG, "handleMessage: $eventName")
        if (eventName == null) return
        if (eventName == "_peers") {
            handleJoinToRoom(map)
        }
        if (eventName == "_new_peer") {
            handleRemoteInRoom(map)
        }
        if (eventName == "_ice_candidate") {
            handleRemoteCandidate(map)
        }
        if (eventName == "_remove_peer") {
            handleRemoteOutRoom(map)
        }
        if (eventName == "_offer") {
            handleOffer(map)
        }
        if (eventName == "_answer") {
            handleAnswer(map)
        }
    }

    private fun handleAnswer(map: Map<*, *>?) {
        Log.i("tuch", " 5  JavaWebSocket  handleAnswer: ")
        val data = map!!["data"] as Map<*, *>?
        val sdpDic: Map<*, *>?
        if (data != null) {
            sdpDic = data["sdp"] as Map<*, *>?
            val socketId = data["socketId"] as String?
            val sdp = sdpDic!!["sdp"] as String?
            events.onReceiverAnswer(socketId, sdp)
        }
    }

    private fun handleRemoteOutRoom(map: Map<*, *>?) {
        Log.i("tuch", "handleRemoteOutRoom: ")
        val data = map!!["data"] as Map<*, *>?
        val socketId: String?
        if (data != null) {
            socketId = data["socketId"] as String?
            events.onRemoteOutRoom(socketId)
        }
    }

    private fun handleOffer(map: Map<*, *>?) {
        Log.i("tuch", "handleOffer: ")
        val data = map!!["data"] as Map<*, *>?
        val sdpDic: Map<*, *>?
        if (data != null) {
            sdpDic = data["sdp"] as Map<*, *>?
            val socketId = data["socketId"] as String?
            val sdp = sdpDic!!["sdp"] as String?
            events.onReceiveOffer(socketId, sdp)
        }
    }

    private fun handleRemoteCandidate(map: Map<*, *>?) {
        Log.i("tuch", "JavaWebSocket  6   handleRemoteCandidate: ")
        val data = map!!["data"] as Map<*, *>?
        val socketId: String?
        if (data != null) {
            socketId = data["socketId"] as String?
            var sdpMid = data["id"] as String?
            sdpMid = sdpMid ?: "video"
            val sdpMLineIndex = data["label"].toString().toDouble().toInt()
            val candidate = data["candidate"] as String?
            val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
            events.onRemoteIceCandidate(socketId, iceCandidate)
        }
    }

    private fun handleRemoteInRoom(map: Map<*, *>?) {
        Log.i("tuch", "     handleRemoteInRoom: ")
        val data = map!!["data"] as Map<*, *>?
        val socketId: String?
        if (data != null) {
            socketId = data["socketId"] as String?
            events.onRemoteJoinToRoom(socketId)
        }
    }

    private fun handleJoinToRoom(map: Map<*, *>?) {
        Log.i("tuch", "  1  JavaWebSocket   handleJoinToRoom: ")
        val data = map!!["data"] as Map<*, *>?
        val arr: JSONArray?
        if (data != null) {
            arr = data["connections"] as JSONArray?
            val js = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName)
            val connections = JSONObject.parseArray(
                js,
                String::class.java
            ) as ArrayList<String?>
            val myId = data["you"] as String?
            events.onJoinToRoom(connections, myId)
        }
    }

    override fun sendIceCandidate(socketId: String?, iceCandidate: IceCandidate?) {
        val childMap: HashMap<String?, Any?> = HashMap()
        childMap["id"] = iceCandidate!!.sdpMid
        childMap["label"] = iceCandidate.sdpMLineIndex
        childMap["candidate"] = iceCandidate.sdp
        childMap["socketId"] = socketId
        val map: HashMap<String?, Any?> = HashMap()
        map["eventName"] = "__ice_candidate"
        map["data"] = childMap
        val `object` = JSONObject(map)
        val jsonString = `object`.toString()
        Log.d(TAG, "send-->$jsonString")
        mWebSocketClient!!.send(jsonString)
    }

    override fun sendAnswer(socketId: String?, sdp: String?) {
        val childMap1: MutableMap<String?, Any?> = HashMap()
        childMap1["type"] = "answer"
        childMap1["sdp"] = sdp
        val childMap2: HashMap<String?, Any?> = HashMap()
        childMap2["socketId"] = socketId
        childMap2["sdp"] = childMap1
        val map: HashMap<String?, Any?> = HashMap()
        map["eventName"] = "__answer"
        map["data"] = childMap2
        val `object` = JSONObject(map)
        val jsonString = `object`.toString()
        Log.d(TAG, "send-->$jsonString")
        mWebSocketClient!!.send(jsonString)
    }

    override fun sendOffer(socketId: String?, sdp: String?) {
        val childMap1: HashMap<String?, Any?> = HashMap()
        childMap1["type"] = "offer"
        childMap1["sdp"] = sdp

        val childMap2: HashMap<String?, Any?> = HashMap()
        childMap2["socketId"] = socketId
        childMap2["sdp"] = childMap1

        val map: HashMap<String?, Any?> = HashMap()
        map["eventName"] = "__offer"
        map["data"] = childMap2

        val `object` = JSONObject(map)
        val jsonString = `object`.toString()
        Log.d(TAG, "send-->$jsonString")
        mWebSocketClient!!.send(jsonString)
    }

    companion object {
        private const val TAG = "JavaWebSocket"
    }
}