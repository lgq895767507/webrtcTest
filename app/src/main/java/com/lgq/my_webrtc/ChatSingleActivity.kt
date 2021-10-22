package com.lgq.my_webrtc

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.lgq.my_webrtc.server.PeerConnectionManager
import com.lgq.my_webrtc.server.WebRTCManager
import com.lgq.my_webrtc.ui.ChatSingleFragment
import com.lgq.my_webrtc.utils.PermissionUtil
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class ChatSingleActivity : AppCompatActivity() {

    private var local_view: SurfaceViewRenderer? = null
    private var remote_view: SurfaceViewRenderer? = null
    private var localRender: ProxyVideoSink? = null
    private var remoteRender: ProxyVideoSink? = null

    private var manager: WebRTCManager? = null

    private var videoEnable = false
    private var isSwappedFeeds = false

    private var rootEglBase: EglBase? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_single)
        initView()
    }

    private fun initView() {
        val intent = intent
        videoEnable = intent.getBooleanExtra("videoEnable", false)

        val chatSingleFragment = ChatSingleFragment()
        replaceFragment(chatSingleFragment, videoEnable)
        rootEglBase = EglBase.create()
        if (videoEnable) {
            local_view = findViewById(R.id.local_view_render)
            remote_view = findViewById(R.id.remote_view_render)

            // 本地图像初始化
            local_view?.init(rootEglBase?.eglBaseContext, null)
            local_view?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            local_view?.setZOrderMediaOverlay(true)
            local_view?.setMirror(true)
            localRender = ProxyVideoSink()
            //远端图像初始化
            remote_view?.init(rootEglBase?.eglBaseContext, null)
            remote_view?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
            remote_view?.setMirror(true)
            remoteRender = ProxyVideoSink()
            setSwappedFeeds(true)
            local_view?.setOnClickListener {
                setSwappedFeeds(
                    !isSwappedFeeds
                )
            }
        }
        startCall()

    }

    private fun startCall() {
        manager = WebRTCManager.getInstance()
        manager?.setCallback(object : IViewCallback {
            override fun onSetLocalStream(stream: MediaStream, socketId: String?) {
                if (stream.videoTracks.size > 0) {
                    stream.videoTracks[0].addSink(localRender)
                }
                if (videoEnable) {
                    stream.videoTracks[0].setEnabled(true)
                }
            }

            override fun onAddRemoteStream(stream: MediaStream, socketId: String?) {
                if (stream.videoTracks.size > 0) {
                    stream.videoTracks[0].addSink(remoteRender)
                }
                if (videoEnable) {
                    stream.videoTracks[0].setEnabled(true)
                    runOnUiThread { setSwappedFeeds(false) }
                }
            }

            override fun onCloseWithId(socketId: String?) {
                runOnUiThread {
                    disConnect()
                    finish()
                }
            }
        })
        if (!PermissionUtil.isNeedRequestPermission(this@ChatSingleActivity)) {
            manager?.joinRoom(applicationContext, rootEglBase!!)
        }
    }

    private fun disConnect() {
        manager!!.exitRoom()
        if (localRender != null) {
            localRender!!.setTarget(null)
            localRender = null
        }
        if (remoteRender != null) {
            remoteRender!!.setTarget(null)
            remoteRender = null
        }
        if (local_view != null) {
            local_view!!.release()
            local_view = null
        }
        if (remote_view != null) {
            remote_view!!.release()
            remote_view = null
        }
    }

    private fun setSwappedFeeds(isSwappedFeeds: Boolean) {
        this.isSwappedFeeds = isSwappedFeeds
        localRender!!.setTarget(if (isSwappedFeeds) remote_view else local_view)
        remoteRender!!.setTarget(if (isSwappedFeeds) local_view else remote_view)
    }

    private fun replaceFragment(fragment: Fragment, videoEnable: Boolean) {
        val bundle = Bundle()
        bundle.putBoolean("videoEnable", videoEnable)
        fragment.arguments = bundle
        val manager = supportFragmentManager
        manager.beginTransaction()
            .replace(R.id.wr_container, fragment)
            .commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event)
    }

    // 切换摄像头
    fun switchCamera() {
        manager!!.switchCamera()
    }

    // 挂断
    fun hangUp() {
        disConnect()
        finish()
    }

    // 静音
    fun toggleMic(enable: Boolean) {
        manager!!.toggleMute(enable)
    }

    // 扬声器
    fun toggleSpeaker(enable: Boolean) {
        manager!!.toggleSpeaker(enable)
    }

    override fun onDestroy() {
        disConnect()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        for (i in permissions.indices) {
            Log.i(
                PeerConnectionManager.TAG,
                "[Permission] " + permissions[i] + " is " + if (grantResults[i] == PackageManager.PERMISSION_GRANTED) "granted" else "denied"
            )
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish()
                break
            }
        }
        manager!!.joinRoom(applicationContext, rootEglBase!!)
    }

    companion object {
        fun openActivity(activity: Activity, videoEnable: Boolean) {
            activity.startActivity(Intent(activity, ChatSingleActivity::class.java).apply {
                putExtra("videoEnable", videoEnable)
            })
        }
    }
}