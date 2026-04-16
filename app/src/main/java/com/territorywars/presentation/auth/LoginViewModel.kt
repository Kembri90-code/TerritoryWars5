package com.territorywars.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territorywars.data.local.TokenDataStore
import com.territorywars.data.remote.api.AuthApi
import com.territorywars.data.remote.dto.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val serverError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
) {
    val isFormValid: Boolean
        get() = email.isNotBlank() && password.isNotBlank()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEmailChanged(value: String) {
        _state.update { it.copy(email = value, emailError = null, serverError = null) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, passwordError = null, serverError = null) }
    }

    fun login() {
        val s = _state.value
        if (!s.isFormValid) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, serverError = null) }
            try {
                val response = authApi.login(LoginRequest(s.email.trim(), s.password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenDataStore.saveTokens(body.accessToken, body.refreshToken)
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    val error = when (response.code()) {
                        401 -> "Неверный email или пароль"
                        404 -> "Пользователь не найден"
                        else -> "Ошибка сервера. Попробуйте позже"
                    }
                    _state.update { it.copy(isLoading = false, serverError = error) }
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is UnknownHostException -> "Нет подключения к интернету"
                    is ConnectException     -> "Сервер недоступен. Попробуйте позже"
                    is SocketTimeoutException -> "Сервер не отвечает. Попробуйте позже"
                    else -> "Ошибка соединения. Попробуйте позже"
                }
                _state.update { it.copy(isLoading = false, serverError = msg) }
            }
        }
    }
}
