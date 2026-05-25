package com.waterman.packai.home.viewmodel

import com.waterman.packai.network.response.SrNoData

sealed class SrNoState {
    object Idle : SrNoState()
    object Loading : SrNoState()
    data class Success(val list: ArrayList<SrNoData>) : SrNoState()
    data class Empty(val message: String) : SrNoState()   // 209
    data class Error(val message: String) : SrNoState()
}