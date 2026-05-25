package com.waterman.packai.home.viewmodel

sealed class UploadAttachmentState {
    object Idle : UploadAttachmentState()
    object Loading : UploadAttachmentState()
    data class Success(
        val message: String
    ) : UploadAttachmentState()
    data class Empty(
        val message: String
    ) : UploadAttachmentState()
    data class Error(
        val message: String
    ) : UploadAttachmentState()
}