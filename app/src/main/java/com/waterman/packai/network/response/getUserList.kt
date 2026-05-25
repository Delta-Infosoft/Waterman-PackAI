package com.waterman.packai.network.response

import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlin.collections.toList
import kotlin.jvm.java

fun LoginResponse.getUserList(): List<LoginUser> {
    return try {
        when {
            // Case 1: result is null
            result == null -> emptyList()

            // Case 2: result is JSON Array (valid success case)
            result.isJsonArray -> {
                val jsonArray = result.asJsonArray
                Gson().fromJson(
                    jsonArray,
                    Array<LoginUser>::class.java
                ).toList()
            }

            // Case 3: result is "" or error string
            result.isJsonPrimitive && result.asJsonPrimitive.isString -> {
                emptyList()
            }

            // Any other unexpected case
            else -> emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}
