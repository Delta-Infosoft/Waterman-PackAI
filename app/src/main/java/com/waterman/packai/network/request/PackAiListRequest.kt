package com.waterman.packai.network.request

import okhttp3.MultipartBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PackAiListRequest(
    val fromDate : Date,val toDate : Date
) {
    private val apiDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    fun toMultipartBody(): MultipartBody {
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("FromDate", apiDateFormatter.format(fromDate))
            .addFormDataPart("ToDate", apiDateFormatter.format(toDate))
            .build()
    }
}