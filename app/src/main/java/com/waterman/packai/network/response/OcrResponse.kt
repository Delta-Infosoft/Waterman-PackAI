package com.waterman.packai.network.response

import com.google.gson.annotations.SerializedName

data class OcrResponse(
    @SerializedName("embossed_text") val embossedText: String? = null,
    @SerializedName("type")         val type: String? = null,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("error")        val error: String? = null
)