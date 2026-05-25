package com.waterman.packai.home.viewmodel

sealed class DeletePhotoState {
    object Idle : DeletePhotoState()
    object Loading : DeletePhotoState()
    data class Success(val message: String) : DeletePhotoState()
    data class Error(val message: String) : DeletePhotoState()
    data class Empty(val message: String) : DeletePhotoState()
}