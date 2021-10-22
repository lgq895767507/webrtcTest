package com.lgq.my_webrtc

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.lgq.my_webrtc.bean.MemberBean
import com.lgq.my_webrtc.server.PeerConnectionManager
import com.lgq.my_webrtc.server.WebRTCManager
import com.lgq.my_webrtc.ui.ChatRoomFragment
import com.lgq.my_webrtc.utils.PermissionUtil
import org.webrtc.*
import java.util.ArrayList
import java.util.HashMap

class ChatRoomActivity : AppCompatActivity(), IViewCallback {

    private var videoView: FrameLayout? = null
    private var mScreenWidth: Int = 0
    private var rootEglBase: EglBase? = null
    private var manager: WebRTCManager? = null
    private var _localVideoTrack: VideoTrack? = null
    private var _videoViews: HashMap<String, SurfaceViewRenderer> = HashMap()
    private val _sinks: HashMap<String, ProxyVideoSink> = HashMap()
    private val _infos: ArrayList<MemberBean> = ArrayList<MemberBean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)
        initView()
        val chatRoomFragment = ChatRoomFragment()
        replaceFragment(chatRoomFragment)
        startCall()
    }

    private fun startCall() {
        manager = WebRTCManager.getInstance()
        manager?.setCallback(this)

        if (!PermissionUtil.isNeedRequestPermission(this@ChatRoomActivity)) {
            rootEglBase?.let { manager?.joinRoom(applicationContext, it) }
        }
    }

    private fun replaceFragment(chatRoomFragment: ChatRoomFragment) {
        val manager = supportFragmentManager
        manager.beginTransaction()
            .replace(R.id.wr_container, chatRoomFragment)
            .commit()
    }

    private fun initView() {
        videoView = findViewById(R.id.wr_video_view)
        val manager = getSystemService(WINDOW_SERVICE) as? WindowManager
        mScreenWidth = manager?.defaultDisplay?.width ?: 0
        videoView?.layoutParams =
            RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mScreenWidth)
        rootEglBase = EglBase.create()
    }


    override fun onSetLocalStream(stream: MediaStream?, userId: String?) {
        val videoTracks = stream!!.videoTracks
        if (videoTracks.size > 0) {
            _localVideoTrack = videoTracks[0]
        }
        runOnUiThread { addView(userId, stream) }
    }

    private fun addView(id: String?, stream: MediaStream?) {
        if (id == null || stream == null) {
            return
        }
        val renderer = SurfaceViewRenderer(this@ChatRoomActivity)
        renderer.init(rootEglBase!!.eglBaseContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        renderer.setMirror(true)
        // set render
        val sink = ProxyVideoSink()
        sink.setTarget(renderer)
        if (stream.videoTracks.size > 0) {
            stream.videoTracks[0].addSink(sink)
        }
        _videoViews[id] = renderer
        _sinks[id] = sink
        _infos.add(MemberBean(id))
        videoView?.addView(renderer)

        val size: Int = _infos.size
        for (i in 0 until size) {
            val memberBean: MemberBean = _infos.get(i)
            val renderer1: SurfaceViewRenderer? = _videoViews[memberBean.id]
            if (renderer1 != null) {
                val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutParams.height = getWidth(size)
                layoutParams.width = getWidth(size)
                layoutParams.leftMargin = getX(size, i)
                layoutParams.topMargin = getY(size, i)
                renderer1.layoutParams = layoutParams
            }
        }
    }

    private fun getY(size: Int, index: Int): Int {
        if (size < 3) {
            return mScreenWidth / 4
        } else if (size < 5) {
            return if (index < 2) {
                0
            } else {
                mScreenWidth / 2
            }
        } else if (size < 7) {
            return if (index < 3) {
                mScreenWidth / 2 - mScreenWidth / 3
            } else {
                mScreenWidth / 2
            }
        } else if (size <= 9) {
            return if (index < 3) {
                0
            } else if (index < 6) {
                mScreenWidth / 3
            } else {
                mScreenWidth / 3 * 2
            }
        }
        return 0
    }

    private fun getX(size: Int, index: Int): Int {
        if (size <= 4) {
            return if (size == 3 && index == 2) {
                mScreenWidth / 4
            } else index % 2 * mScreenWidth / 2
        } else if (size <= 9) {
            if (size == 5) {
                if (index == 3) {
                    return mScreenWidth / 6
                }
                if (index == 4) {
                    return mScreenWidth / 2
                }
            }
            if (size == 7 && index == 6) {
                return mScreenWidth / 3
            }
            if (size == 8) {
                if (index == 6) {
                    return mScreenWidth / 6
                }
                if (index == 7) {
                    return mScreenWidth / 2
                }
            }
            return index % 3 * mScreenWidth / 3
        }
        return 0
    }

    private fun getWidth(size: Int): Int {
        if (size <= 4) {
            return mScreenWidth / 2
        } else if (size <= 9) {
            return mScreenWidth / 3
        }
        return mScreenWidth / 3
    }

    override fun onAddRemoteStream(stream: MediaStream?, socketId: String?) {
        runOnUiThread {
            addView(socketId, stream)
        }
    }

    override fun onCloseWithId(socketId: String?) {
        runOnUiThread {
            removeView(socketId)
        }
    }

    private fun removeView(userId: String?) {
        if (userId == null) {
            return
        }
        val sink = _sinks[userId]
        val renderer = _videoViews[userId]
        sink?.setTarget(null)
        renderer?.release()
        _sinks.remove(userId)
        _videoViews.remove(userId)
        _infos.remove(MemberBean(userId))
        videoView?.removeView(renderer)


        val size = _infos.size
        for (i in _infos.indices) {
            val memberBean = _infos[i]
            val renderer1 = _videoViews[memberBean.id]
            if (renderer1 != null) {
                val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutParams.height = getWidth(size)
                layoutParams.width = getWidth(size)
                layoutParams.leftMargin = getX(size, i)
                layoutParams.topMargin = getY(size, i)
                renderer1.layoutParams = layoutParams
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        exit()
        super.onDestroy()
    }

    // 切换摄像头
    fun switchCamera() {
        manager!!.switchCamera()
    }

    // 挂断
    fun hangUp() {
        exit()
        finish()
    }

    // 静音
    fun toggleMic(enable: Boolean) {
        manager!!.toggleMute(enable)
    }

    // 免提
    fun toggleSpeaker(enable: Boolean) {
        manager!!.toggleSpeaker(enable)
    }

    // 打开关闭摄像头
    fun toggleCamera(enableCamera: Boolean) {
        if (_localVideoTrack != null) {
            _localVideoTrack!!.setEnabled(enableCamera)
        }
    }

    private fun exit() {
        manager!!.exitRoom()
        for (renderer in _videoViews.values) {
            renderer.release()
        }
        for (sink in _sinks.values) {
            sink.setTarget(null)
        }
        _videoViews.clear()
        _sinks.clear()
        _infos.clear()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
        fun openActivity(activity: Activity) {
            activity.startActivity(Intent(activity, ChatRoomActivity::class.java))
        }
    }
}