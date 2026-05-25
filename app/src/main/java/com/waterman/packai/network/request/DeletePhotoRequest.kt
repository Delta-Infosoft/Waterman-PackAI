package com.waterman.packai.network.request

import okhttp3.MultipartBody

data class DeletePhotoRequest(
    val fUId: String)
{
    fun toMultipartBody(): MultipartBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("FUId", fUId)
            .build()
    }
}