package com.lgq.my_webrtc

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText

class MainActivity : AppCompatActivity() {

    private var serverEt: AppCompatEditText? = null
    private var roomEt: AppCompatEditText? = null
    private var portEt: AppCompatEditText? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        serverEt = findViewById(R.id.server_et)
        roomEt = findViewById(R.id.room_et)
        portEt = findViewById(R.id.port_et)
    }

    fun JoinRoom(view: View) {
        WebrtcUtil.call(
            this,
            serverEt?.text.toString(),
            roomEt?.text.toString().trim { it <= ' ' })
    }

    fun JoinRoomSingleVideo(view: View) {
        WebrtcUtil.callSingle(this,
            serverEt?.text.toString(),
            roomEt?.text.toString().trim { it <= ' ' } + ":" + portEt?.text.toString()
                .trim { it <= ' ' },
            true
        )
    }

    fun JoinRoomSingleAudio(view: View) {
        WebrtcUtil.callSingle(this,
            serverEt?.text.toString(),
            roomEt?.text.toString().trim { it <= ' ' } + ":" + portEt?.text.toString()
                .trim { it <= ' ' },
            false
        )
    }
}