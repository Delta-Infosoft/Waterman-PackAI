package com.waterman.packai.home.viewmodel

import com.waterman.packai.network.response.ProductList

sealed class ProductListState {
    object Idle : ProductListState()
    object Loading : ProductListState()
    data class Success(val data: List<ProductList>) : ProductListState()
    data class Error(val message: String) : ProductListState()
    data class Empty(val message: String) : ProductListState()
}