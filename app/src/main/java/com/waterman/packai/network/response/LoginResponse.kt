package com.waterman.packai.network.response

import androidx.annotation.Keep
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

@Keep
data class LoginResponse(
    val status: String,
    val message: String,
    val result: JsonElement?
)

@Keep
data class LoginUser(
    @SerializedName("UserId"             ) var UserId             : String? = null,
    @SerializedName("FirstName"          ) var FirstName          : String? = null,
    @SerializedName("LastName"           ) var LastName           : String? = null,
    @SerializedName("UsersName"          ) var UsersName          : String? = null,
    @SerializedName("MobileNo"           ) var MobileNo           : String? = null,
    @SerializedName("EmailId"            ) var EmailId            : String? = null,
    @SerializedName("IMEICode"           ) var IMEICode           : String? = null,
    @SerializedName("FCMId"              ) var FCMId              : String? = null,
    @SerializedName("IsAdmin"            ) var IsAdmin            : String? = null,
    @SerializedName("LastLoginDateTime"  ) var LastLoginDateTime  : String? = null,
    @SerializedName("LastLogOutDateTime" ) var LastLogOutDateTime : String? = null
)
