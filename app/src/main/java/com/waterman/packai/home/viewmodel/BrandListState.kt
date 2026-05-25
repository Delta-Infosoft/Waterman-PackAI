package com.waterman.packai.home.viewmodel

import com.waterman.packai.network.response.ApiResponse
import com.waterman.packai.network.response.BrandList

sealed class BrandListState {
    object Idle : BrandListState()
    object Loading : BrandListState()
    data class Success(val data: ArrayList<BrandList>) : BrandListState()
    data class Empty(val message: String) : BrandListState()
    data class Error(val message: String) : BrandListState()
}