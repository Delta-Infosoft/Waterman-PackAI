package com.waterman.packai.network.request

import okhttp3.MultipartBody

data class LogoutRequest(
    val userName: String,
    val imei: String
)
{
    fun toMultipartBody(): MultipartBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("UserName", userName)
            .addFormDataPart("IMEI", imei)
            .build()
    }
}