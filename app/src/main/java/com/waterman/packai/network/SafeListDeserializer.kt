package com.waterman.packai.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.lang.reflect.ParameterizedType

class SafeListDeserializer<T> : JsonDeserializer<List<T>> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<T> {

        if (json == null || json.isJsonNull) {
            return emptyList()
        }

        // Handle: "result": ""
        if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
            return emptyList()
        }

        // Handle normal array safely (NO recursion)
        if (json.isJsonArray) {
            val itemType = (typeOfT as ParameterizedType).actualTypeArguments[0]
            val list = mutableListOf<T>()

            for (element in json.asJsonArray) {
                list.add(context!!.deserialize(element, itemType))
            }
            return list
        }

        return emptyList()
    }
}