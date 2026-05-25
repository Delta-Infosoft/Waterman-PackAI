package com.waterman.packai.network.response

import com.google.gson.annotations.SerializedName

data class SrNoDataResponse (
  @SerializedName("status"  ) var status  : String?           = null,
  @SerializedName("message" ) var message : String?           = null,
  @SerializedName("result")  var result: List<SrNoData>? = null
)

data class SrNoData (
  @SerializedName("VourcherNo"   ) var VourcherNo   : String? = null,
  @SerializedName("VoucherDt"    ) var VoucherDt    : String? = null,
  @SerializedName("SONo"         ) var SONo         : String? = null,
  @SerializedName("SODt"         ) var SODt         : String? = null,
  @SerializedName("CustomerName" ) var CustomerName : String? = null,
  @SerializedName("SubGrpName"   ) var SubGrpName   : String? = null,
  @SerializedName("ItmCode"      ) var ItmCode      : String? = null,
  @SerializedName("ItmName"      ) var ItmName      : String? = null,
  @SerializedName("PrintName"    ) var PrintName    : String? = null,
  @SerializedName("SoLnRemarks"  ) var SoLnRemarks  : String? = null,
  @SerializedName("SerialNo"     ) var SerialNo     : String? = null,
  @SerializedName("UMSymbolPri"  ) var UMSymbolPri  : String? = null,
  @SerializedName("Star"         ) var Star         : String? = null,
  @SerializedName("voltage"      ) var voltage      : String? = null,
  @SerializedName("HP"           ) var HP           : String? = null,
  @SerializedName("Stage"        ) var Stage        : String? = null,
  @SerializedName("Phase"        ) var Phase        : String? = null,
  @SerializedName("DelSize"      ) var DelSize      : String? = null,
  @SerializedName("HeadRangeMin" ) var HeadRangeMin : String? = null,
  @SerializedName("HeadRangeMax" ) var HeadRangeMax : String? = null,
  @SerializedName("kw"           ) var kw           : String? = null,
  @SerializedName("IPkW"         ) var IPkW         : String? = null,
  @SerializedName("MRP"          ) var MRP          : String? = null,
  @SerializedName("Boresize"     ) var Boresize     : String? = null,
  @SerializedName("MaxCurrent"   ) var MaxCurrent   : String? = null,
  @SerializedName("Frequency"    ) var Frequency    : String? = null,
  @SerializedName("EANCode"      ) var EANCode      : String? = null,
  @SerializedName("rpm"          ) var rpm          : String? = null,
  @SerializedName("Discharge"    ) var Discharge    : String? = null,
  @SerializedName("PackAIEntryId"    ) var PackAIEntryId    : String? = null
)