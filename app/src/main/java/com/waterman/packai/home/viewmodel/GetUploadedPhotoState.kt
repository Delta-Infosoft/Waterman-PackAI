package com.waterman.packai.home.viewmodel

import com.waterman.packai.network.response.PhotoItem

sealed class GetUploadedPhotoState {
    object Idle : GetUploadedPhotoState()
    data class Loading(val formType: String) : GetUploadedPhotoState()
    data class Success(
        val formType: String,
        val data: List<PhotoItem>
    ) : GetUploadedPhotoState()
    data class Error(
        val formType: String,
        val message: String
    ) : GetUploadedPhotoState()
    data class Empty(
        val formType: String,
        val message: String
    ) : GetUploadedPhotoState()
}
