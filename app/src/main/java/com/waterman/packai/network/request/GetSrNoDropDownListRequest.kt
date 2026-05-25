package com.waterman.packai.network.request

import okhttp3.MultipartBody

data class GetSrNoDropDownListRequest(
    val brandId: String
) {
    fun toMultipartBody(): MultipartBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("BrandId", brandId)
            .build()
    }
}