package com.waterman.packai.authentication.viewmodel

sealed class LogoutState {
    object Idle : LogoutState()
    object Loading : LogoutState()
    data class Success(val message: String?) : LogoutState()
    data class Error(val message: String) : LogoutState()
}