package com.lgq.my_webrtc.server

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class TrustManagerTest : X509TrustManager {

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }


    override fun getAcceptedIssuers(): Array<X509Certificate> {
        TODO("Not yet implemented")
    }

}
