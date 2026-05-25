package com.waterman.packai.home.viewmodel

sealed class SavePackState {
    object Idle : SavePackState()
    object Loading : SavePackState()
    data class Success(val message: String) : SavePackState()
    data class Error(val message: String) : SavePackState()
}
