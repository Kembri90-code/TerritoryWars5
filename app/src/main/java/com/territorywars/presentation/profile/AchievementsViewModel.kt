package com.territorywars.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territorywars.data.remote.api.AchievementApi
import com.territorywars.domain.model.Achievement
import com.territorywars.domain.model.AchievementCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementsState(
    val all: List<Achievement> = emptyList(),
    val unlocked: List<Achievement> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalPoints: Int = 0,
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementApi: AchievementApi,
) : ViewModel() {

    private val _state = MutableStateFlow(AchievementsState())
    val state: StateFlow<AchievementsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val allResp = achievementApi.getAllAchievements()
                val meResp  = achievementApi.getMyAchievements()

                if (!allResp.isSuccessful || !meResp.isSuccessful) {
                    _state.update { it.copy(isLoading = false, error = "Не удалось загрузить достижения") }
                    return@launch
                }

                val allDtos      = allResp.body().orEmpty()
                val unlockedDtos = meResp.body().orEmpty()
                val unlockedIds  = unlockedDtos.map { it.id }.toSet()

                val allDomain      = allDtos.map { dto ->
                    dto.copy(unlockedAt = unlockedDtos.find { it.id == dto.id }?.unlockedAt)
                        .toDomain()
                }
                val unlockedDomain = allDomain.filter { it.isUnlocked }
                val totalPoints    = unlockedDomain.sumOf { it.points }

                _state.update {
                    it.copy(
                        isLoading    = false,
                        all          = allDomain,
                        unlocked     = unlockedDomain,
                        totalPoints  = totalPoints,
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, error = "Нет подключения") }
            }
        }
    }

    fun achievementsByCategory(): Map<AchievementCategory, List<Achievement>> =
        state.value.all.groupBy { it.category }
}
