package com.territorywars.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territorywars.data.local.TokenDataStore
import com.territorywars.data.remote.api.UserApi
import com.territorywars.data.remote.dto.UpdateProfileRequest
import com.territorywars.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userApi: UserApi,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = userApi.getMe()
                if (response.isSuccessful) {
                    _state.update { it.copy(user = response.body()!!.toDomain(), isLoading = false) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Не удалось загрузить профиль") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Нет подключения к серверу") }
            }
        }
    }

    fun changeColor(hexColor: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val response = userApi.updateProfile(
                    UpdateProfileRequest(username = null, color = hexColor, cityId = null)
                )
                if (response.isSuccessful) {
                    _state.update { it.copy(user = response.body()!!.toDomain(), isSaving = false) }
                } else {
                    _state.update { it.copy(isSaving = false) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenDataStore.clearTokens()
            _state.update { it.copy(isLoggedOut = true) }
        }
    }
}
