package com.waterman.packai.network.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


data class ProductListResponse (
  @SerializedName("status"  ) var status  : String?           = null,
  @SerializedName("message" ) var message : String?           = null,
  @SerializedName("result")  val result:  Any?    = null  // ✅ Fix: Any? instead of ArrayList<TourAgendaTrackingFacets>
)

@Parcelize
data class ProductList(
  @SerializedName("PackAIEntryId") var PackAIEntryId: String? = null,
  @SerializedName("SrNo") var SrNo: String? = null,
  @SerializedName("SONo") var SONo: String? = null,
  @SerializedName("CustomerName") var CustomerName: String? = null,
  @SerializedName("ItemName") var ItemName: String? = null,

  @SerializedName("MotorSerialNoStatus") var MotorSerialNoStatus: String? = null,
  @SerializedName("PumpSerialNoStatus") var PumpSerialNoStatus: String? = null,
  @SerializedName("TopBodyStatus") var TopBodyStatus: String? = null,

  @SerializedName("Remarks") var Remarks: String? = null,
  @SerializedName("InsertedOn") var InsertedOn: String? = null,
  @SerializedName("InsertedbyUserId") var InsertedbyUserId: String? = null,

  @SerializedName("ExtractedPumpSrNo") var ExtractedPumpSrNo: String? = null,
  @SerializedName("ExtractedMotorSrNo") var ExtractedMotorSrNo: String? = null,

  @SerializedName("ExtractedPumpType") var ExtractedPumpType: String? = null,
  @SerializedName("ExtractedMotorType") var ExtractedMotorType: String? = null,

  @SerializedName("PumpTypeStatus") var PumpTypeStatus: String? = null,
  @SerializedName("MotorTypeStatus") var MotorTypeStatus: String? = null,

  @SerializedName("BrandId") var BrandId: String? = null,
  @SerializedName("FullAiTextPump") var FullAiTextPump: String? = null,
  @SerializedName("FullAiTextMotor") var FullAiTextMotor: String? = null,
  @SerializedName("FullAiTextLogo") var FullAiTextLogo: String? = null,

  @SerializedName("LastUpdatedByUserId") var LastUpdatedByUserId: String? = null,
  @SerializedName("LastUpdatedOn") var LastUpdatedOn: String? = null

) : Parcelable