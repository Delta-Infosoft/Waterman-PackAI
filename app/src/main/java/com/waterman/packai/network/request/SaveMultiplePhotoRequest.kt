package com.waterman.packai.network.request

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

data class SaveMultiplePhotoRequest(
    val userId: String,
    val photo: File?,
    val packAiEntryId: String
) {

    fun toMultipartBody(): MultipartBody {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            // Text Parts
            .addFormDataPart("UserId", userId)
            .addFormDataPart("RecordId", packAiEntryId)

        photo?.let {
            builder.addFormDataPart(
                "Photo",
                it.name,
                it.asRequestBody("image/*".toMediaTypeOrNull())
            )
        }
        return builder.build()
    }
}
