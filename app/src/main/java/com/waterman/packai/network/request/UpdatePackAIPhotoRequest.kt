package com.waterman.packai.network.request

import okhttp3.MultipartBody

data class UpdatePackAIRequest(
    val packAIEntryId: String? = null,
    val userId: String? = null,
    val type: String? = null,
    val pumpSerialNoStatus: String? = null,
    val extractedPumpSrNo: String? = null,
    val motorSerialNoStatus: String? = null,
    val extractedMotorSrNo: String? = null,
    val pumpTypeStatus: String? = null,
    val extractedPumpType: String? = null,
    val motorTypeStatus: String? = null,
    val extractedMotorType: String? = null
) {

    fun toMultipartBody(): MultipartBody {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

        packAIEntryId?.let { builder.addFormDataPart("PackAIEntryId", it) }
        userId?.let { builder.addFormDataPart("UserId", it) }
        type?.let { builder.addFormDataPart("Type", it) }
        pumpSerialNoStatus?.let { builder.addFormDataPart("PumpSerialNoStatus", it) }
        extractedPumpSrNo?.let { builder.addFormDataPart("ExtractedPumpSrNo", it) }
        motorSerialNoStatus?.let { builder.addFormDataPart("MotorSerialNoStatus", it) }
        extractedMotorSrNo?.let { builder.addFormDataPart("ExtractedMotorSrNo", it) }
        pumpTypeStatus?.let { builder.addFormDataPart("PumpTypeStatus", it) }
        extractedPumpType?.let { builder.addFormDataPart("ExtractedPumpType", it) }
        motorTypeStatus?.let { builder.addFormDataPart("MotorTypeStatus", it) }
        extractedMotorType?.let { builder.addFormDataPart("ExtractedMotorType", it) }

        return builder.build()
    }
}