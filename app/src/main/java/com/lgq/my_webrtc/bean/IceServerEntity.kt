package com.lgq.my_webrtc.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * @author : liuguoqing
 * time   : 2021/10/22
 * desc   :
 */

@Parcelize
data class IceServerEntity(
    val uri: String,
    val username: String,
    val password: String
): Parcelable