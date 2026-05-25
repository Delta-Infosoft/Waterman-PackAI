package com.waterman.packai.authentication.viewmodel

import com.waterman.packai.network.request.GetUserValidRequest
import com.waterman.packai.network.request.LoginRequest
import com.waterman.packai.network.request.LogoutRequest
import com.waterman.packai.network.response.LoginResponse
import com.waterman.packai.network.service.ApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Named

class AuthenticationRepository @Inject constructor(@Named("DEFAULT")private val apiService: ApiService) {

    suspend fun checkUser(request: GetUserValidRequest): Response<LoginResponse> {
        return apiService.checkUser(request.toMultipartBody())
    }

    suspend fun loginWithFcmId(request: LoginRequest): Response<LoginResponse> {
        return apiService.loginWithFcmId(request.toMultipartBody())
    }

    suspend fun logOutWithFcmId(request: LogoutRequest): Response<LoginResponse> {
        return apiService.logOutWithFcmId(request.toMultipartBody())
    }
}