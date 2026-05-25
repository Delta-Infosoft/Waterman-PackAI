
package com.waterman.packai.home.viewmodel

import com.waterman.packai.network.response.ApiResponse

sealed class SrNoListState {
    object Idle : SrNoListState()
    object Loading : SrNoListState()
    data class Success(val data: ApiResponse) : SrNoListState()
    data class Empty(val message: String) : SrNoListState()
    data class Error(val message: String) : SrNoListState()
}