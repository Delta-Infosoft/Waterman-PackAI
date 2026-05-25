package com.waterman.packai.home.viewmodel

sealed class UpdatePackAIState {
    object Idle : UpdatePackAIState()
    object Loading : UpdatePackAIState()
    data class Success(val message: String) : UpdatePackAIState()
    data class Error(val message: String) : UpdatePackAIState()
    data class Empty(val message: String) : UpdatePackAIState()
}