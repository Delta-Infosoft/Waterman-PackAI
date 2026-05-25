package com.waterman.packai.network.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class BrandListResponse (

  @SerializedName("status"  ) var status  : String?           = null,
  @SerializedName("message" ) var message : String?           = null,
  @SerializedName("result"  ) var result  : ArrayList<BrandList> = arrayListOf()

)

@Parcelize
data class BrandList(

  @SerializedName("BrandId" ) var BrandId : String? = null,
  @SerializedName("Text"    ) var Text    : String? = null

): Parcelable