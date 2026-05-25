package com.waterman.packai.network.request

import androidx.annotation.Keep
import okhttp3.MultipartBody

@Keep
data class LoginRequest(
    val userName: String,
    val password: String,
    val imei: String,
    val fcmId: String
) {
    fun toMultipartBody(): MultipartBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("UserName", userName)
            .addFormDataPart("Password", password)
            .addFormDataPart("IMEI", imei)
            .addFormDataPart("FCMId", fcmId)
            .build()
    }
}
