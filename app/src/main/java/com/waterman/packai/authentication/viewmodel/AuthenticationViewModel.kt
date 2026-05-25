package com.waterman.packai.authentication.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waterman.packai.home.viewmodel.SrNoState
import com.waterman.packai.network.NetworkConnectionInterceptor
import com.waterman.packai.network.request.GetUserValidRequest
import com.waterman.packai.network.request.LoginRequest
import com.waterman.packai.network.request.LogoutRequest
import com.waterman.packai.network.response.getUserList
import com.waterman.packai.utils.Constants
import com.waterman.packai.utils.EncryptedPrefHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val repository: AuthenticationRepository,
    private val prefHelper: EncryptedPrefHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(userName: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            try {
                // ---------------------------
                // 1️⃣ CHECK USER API
                // ---------------------------
                val checkResponse = repository.checkUser(
                    GetUserValidRequest(
                        UserId = userName,
                        Password = password
                    )
                )

                if (!checkResponse.isSuccessful || checkResponse.body() == null) {
                    _loginState.value = LoginState.Error("Server error")
                    return@launch
                }

                val checkBody = checkResponse.body()!!

                // 🔥 BUSINESS STATUS CHECK
                if (checkBody.status != "200") {
                    _loginState.value = LoginState.Error(checkBody.message)
                    return@launch
                }

                // ---------------------------
                // 2️⃣ LOGIN API
                // ---------------------------
                val response = repository.loginWithFcmId(
                    LoginRequest(
                        userName = userName,
                        password = password,
                        imei = Constants.getDeviceId(context),
                        fcmId = prefHelper.getFCMToken()
                    )
                )

                if (!response.isSuccessful || response.body() == null) {
                    _loginState.value = LoginState.Error("Server error")
                    return@launch
                }

                val body = response.body()!!

                // ---------------------------
                // STATUS HANDLING
                // ---------------------------
                when (body.status) {

                    // ✅ SUCCESS
                    "200" -> {
                        val users = body.getUserList()

                        if (users.isEmpty()) {
                            _loginState.value = LoginState.Error(body.message)
                            return@launch
                        }

                        val user = users.first()
                        prefHelper.saveUser(user)

                        _loginState.value = LoginState.Success(user)
                    }

                    // ⚠️ APPROVAL REQUIRED
                    "209" -> {
                        _loginState.value =
                            LoginState.ApprovalRequired(body.message)
                    }

                    // ❌ OTHER ERRORS
                    else -> {
                        _loginState.value =
                            LoginState.Error(body.message)
                    }
                }

            } catch (e: Exception) {
                when (e) {
                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _loginState.value = LoginState.Error("No Internet Connection")
                    }

                    is SocketTimeoutException -> {
                        _loginState.value = LoginState.Error("Connection Timeout")
                    }

                    is IOException -> {
                        _loginState.value = LoginState.Error("Network Error")
                    }

                    else -> {
                        _loginState.value =
                            LoginState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }

    // =========================================================================================
    // ========================== Log out API =============================================
    private val _logoutState = MutableLiveData<LogoutState>(LogoutState.Idle)
    val logoutState: LiveData<LogoutState> = _logoutState
    fun logout(userName: String, imei: String) {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Loading

            try {
                val request = LogoutRequest(userName = userName, imei = imei)

                val response = repository.logOutWithFcmId(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    if (body.status == "200" || body.status == "209") {
                        _logoutState.value = LogoutState.Success(body.message)
                    } else {
                        _logoutState.value = LogoutState.Error(body.message ?: "Logout failed")
                    }
                } else {
                    _logoutState.value =
                        LogoutState.Error("Unable to connect server")
                }

            } catch (e: Exception) {
                when (e) {
                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _logoutState.value = LogoutState.Error("No Internet Connection")
                    }

                    is SocketTimeoutException -> {
                        _logoutState.value = LogoutState.Error("Connection Timeout")
                    }

                    is IOException -> {
                        _logoutState.value = LogoutState.Error("Network Error")
                    }

                    else -> {
                        _logoutState.value =
                            LogoutState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }

    fun clearLocalData() {
        // 🧹 Clear encrypted prefs
        prefHelper.clear()
    }


}
