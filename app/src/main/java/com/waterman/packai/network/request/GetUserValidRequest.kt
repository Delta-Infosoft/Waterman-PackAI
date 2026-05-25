package com.waterman.packai.network.request

import androidx.annotation.Keep
import okhttp3.MultipartBody
@Keep
data class GetUserValidRequest(
    val UserId: String,
    val Password: String,
) {
    fun toMultipartBody(): MultipartBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("UserId", UserId)
            .addFormDataPart("Password", Password)
            .build()
    }
}