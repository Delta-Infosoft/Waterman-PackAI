package com.waterman.packai.home.viewmodel

import com.waterman.packai.network.response.OcrResponse

sealed class OcrState {
    object Idle : OcrState()
    object Loading : OcrState()
    data class Success(
        val data: OcrResponse,
        val sourceType: String    // "Pump" | "Motor" | "BodyScan"
    ) : OcrState()
    data class Error(val message: String, val sourceType: String = "") : OcrState()
}