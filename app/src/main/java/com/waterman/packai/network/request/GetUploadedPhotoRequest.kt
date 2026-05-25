package com.waterman.packai.network.request

import okhttp3.MultipartBody

data class GetUploadedPhotoRequest(
    val recordId: String,
    val formName: String,
)
{
    fun toMultipartBody(): MultipartBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("RecordId", recordId)
            .addFormDataPart("FormName", formName)
            .build()
    }
}