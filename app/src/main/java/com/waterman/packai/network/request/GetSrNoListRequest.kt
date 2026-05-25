package com.waterman.packai.network.request

import okhttp3.MultipartBody

data class GetSrNoListRequest(
    val SrNo: String
) {
    fun toMultipartBody(): MultipartBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("SrNo", SrNo)
            .build()
    }
}