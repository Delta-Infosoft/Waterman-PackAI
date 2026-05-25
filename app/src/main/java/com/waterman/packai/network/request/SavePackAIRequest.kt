package com.waterman.packai.network.request

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

data class SavePackAIRequest(
    val brandId: String,
    val userId: String,
    val srNo: String,
    val sONo: String,
    val customerName: String,
    val itemName: String,
    val motorSerialNoStatus: String,
    val pumpSerialNoStatus: String,
    val topBodyStatus: String,

    val extractedPumpSrNo: String,
    val extractedMotorSrNo: String,
    val extractedPumpType: String,
    val extractedMotorType: String,

    val fullAiPumpText: String,
    val fullAiMotorText: String,
    val fullAiLogoText: String,

    val pumpTypeStatus: String,
    val motorTypeStatus: String,

    val mainPhoto: File?,
    val topPhoto: File?,
    val motorPhoto: File?,
    val pumpPhoto: File?,

    val motorMainPhoto: File?,
    val pumpSetPhoto: File?,

    val itemArray: String,
    val remark: String,
    val packAiEntryId: String
) {

    fun toMultipartBody(): MultipartBody {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)

            // Text Parts
            .addFormDataPart("UserId", userId)
            .addFormDataPart("BrandId", brandId)
            .addFormDataPart("SrNo", srNo)
            .addFormDataPart("SONo", sONo)
            .addFormDataPart("CustomerName", customerName)
            .addFormDataPart("ItemName", itemName)
            .addFormDataPart("MotorSerialNoStatus", motorSerialNoStatus)
            .addFormDataPart("PumpSerialNoStatus", pumpSerialNoStatus)
            .addFormDataPart("TopBodyStatus", topBodyStatus)
            .addFormDataPart("ItemArray", itemArray)
            .addFormDataPart("Remarks", remark)
            .addFormDataPart("PackAIEntryId", packAiEntryId)

            .addFormDataPart("ExtractedPumpSrNo", extractedPumpSrNo)
            .addFormDataPart("ExtractedMotorSrNo", extractedMotorSrNo)
            .addFormDataPart("ExtractedPumpType", extractedPumpType)
            .addFormDataPart("ExtractedMotorType", extractedMotorType)

            .addFormDataPart("FullAiTextPump", fullAiPumpText)
            .addFormDataPart("FullAiTextMotor", fullAiMotorText)
            .addFormDataPart("FullAiTextLogo", fullAiLogoText)

            .addFormDataPart("PumpTypeStatus", pumpTypeStatus)
            .addFormDataPart("MotorTypeStatus", motorTypeStatus)

        mainPhoto?.let {
            builder.addFormDataPart(
                "MainPhoto",
                it.name,
                it.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        topPhoto?.let {
            builder.addFormDataPart(
                "TopPhoto",
                it.name,
                it.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        motorPhoto?.let {
            builder.addFormDataPart(
                "MotorPhoto",
                it.name,
                it.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        pumpPhoto?.let {
            builder.addFormDataPart(
                "PumpPhoto",
                it.name,
                it.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        motorMainPhoto?.let {
            builder.addFormDataPart(
                "MotorMainPhoto",
                it.name,
                it.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        pumpSetPhoto?.let {
            builder.addFormDataPart(
                "PumpSetPhoto",
                it.name,
                it.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }

        return builder.build()
    }
}
