package com.waterman.packai.authentication.viewmodel

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: Any) : LoginState()
    data class Error(val message: String) : LoginState()

    data class ApprovalRequired(val message: String) : LoginState()
}
