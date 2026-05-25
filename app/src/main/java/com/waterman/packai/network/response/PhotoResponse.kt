package com.waterman.packai.network.response

import com.google.gson.annotations.SerializedName

data class PhotoResponse(
    @SerializedName("status") var status: String? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("result") var result: List<PhotoItem>? = null
)

data class PhotoItem(
    @SerializedName("LnNo") var lnNo: String? = null,
    @SerializedName("FUId") var fUId: String? = null,
    @SerializedName("RecordId") var recordId: String? = null,
    @SerializedName("FilePath") var filePath: String? = null,
    @SerializedName("InsertedOn") var insertedOn: String? = null,
    @SerializedName("FormName") var formName: String? = null
)
