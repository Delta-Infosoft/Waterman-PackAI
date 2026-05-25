package com.waterman.packai.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class HtmlStripInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response

        val raw = body.string()

        // Take ONLY JSON part
        val cleanJson = raw.substringBefore("<!DOCTYPE")

        val newBody = cleanJson.toResponseBody(body.contentType())

        return response.newBuilder()
            .body(newBody)
            .build()
    }
}
