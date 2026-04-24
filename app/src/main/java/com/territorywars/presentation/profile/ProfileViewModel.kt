package com.territorywars.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territorywars.data.local.TokenDataStore
import com.territorywars.data.remote.api.UserApi
import com.territorywars.data.remote.dto.UpdateProfileRequest
import com.territorywars.domain.model.User
import com.territorywars.presentation.map.PlayerMarker
import com.territorywars.presentation.map.playerMarkerById
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
    val selectedMarker: PlayerMarker = PlayerMarker.DOT
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userApi: UserApi,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
        viewModelScope.launch {
            tokenDataStore.playerMarker.collect { id ->
                _state.update { it.copy(selectedMarker = playerMarkerById(id)) }
            }
        }
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

    fun changeMarker(marker: PlayerMarker) {
        viewModelScope.launch {
            tokenDataStore.savePlayerMarker(marker.id)
            _state.update { it.copy(selectedMarker = marker) }
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: run {
                    _state.update { it.copy(isSaving = false, error = "Не удалось открыть файл") }
                    return@launch
                }
                val bytes = stream.readBytes()
                stream.close()
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when (mimeType) {
                    "image/png"  -> "png"
                    "image/webp" -> "webp"
                    else         -> "jpg"
                }
                val part = MultipartBody.Part.createFormData(
                    "avatar", "avatar.$ext",
                    bytes.toRequestBody(mimeType.toMediaType())
                )
                val response = userApi.uploadAvatar(part)
                if (response.isSuccessful) {
                    _state.update { it.copy(user = response.body()!!.toDomain(), isSaving = false) }
                } else {
                    _state.update { it.copy(isSaving = false, error = "Не удалось загрузить фото") }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, error = "Ошибка загрузки") }
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
