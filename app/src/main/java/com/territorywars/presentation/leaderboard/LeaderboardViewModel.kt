package com.territorywars.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territorywars.data.remote.api.ClanLeaderboardDto
import com.territorywars.data.remote.api.LeaderboardApi
import com.territorywars.data.remote.api.PlayerLeaderboardDto
import com.territorywars.domain.model.ClanLeaderboardEntry
import com.territorywars.domain.model.PlayerLeaderboardEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlayerSort(val label: String, val apiValue: String) {
    AREA("По площади", "area"),
    CAPTURES("По захватам", "captures"),
    DISTANCE("По дистанции", "distance")
}

data class LeaderboardState(
    val selectedTab: Int = 0,           // 0 = Игроки, 1 = Кланы
    val playerSort: PlayerSort = PlayerSort.AREA,
    val players: List<PlayerLeaderboardEntry> = emptyList(),
    val clans: List<ClanLeaderboardEntry> = emptyList(),
    val isLoadingPlayers: Boolean = false,
    val isLoadingClans: Boolean = false,
    val playersError: String? = null,
    val clansError: String? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardApi: LeaderboardApi
) : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardState())
    val state: StateFlow<LeaderboardState> = _state.asStateFlow()

    init {
        loadPlayers(PlayerSort.AREA)
        loadClans()
    }

    fun onTabSelected(index: Int) {
        _state.update { it.copy(selectedTab = index) }
    }

    fun onPlayerSortChanged(sort: PlayerSort) {
        _state.update { it.copy(playerSort = sort) }
        loadPlayers(sort)
    }

    fun loadPlayers(sort: PlayerSort = _state.value.playerSort) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPlayers = true, playersError = null) }
            try {
                val response = leaderboardApi.getPlayersLeaderboard(sort = sort.apiValue, limit = 100)
                if (response.isSuccessful) {
                    _state.update {
                        it.copy(
                            players = response.body()!!.map { dto -> dto.toDomain() },
                            isLoadingPlayers = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoadingPlayers = false, playersError = "Ошибка загрузки") }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingPlayers = false, playersError = "Нет подключения к серверу") }
            }
        }
    }

    fun loadClans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingClans = true, clansError = null) }
            try {
                val response = leaderboardApi.getClansLeaderboard(sort = "area", limit = 50)
                if (response.isSuccessful) {
                    _state.update {
                        it.copy(
                            clans = response.body()!!.map { dto -> dto.toDomain() },
                            isLoadingClans = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoadingClans = false, clansError = "Ошибка загрузки") }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingClans = false, clansError = "Нет подключения к серверу") }
            }
        }
    }
}

private fun PlayerLeaderboardDto.toDomain() = PlayerLeaderboardEntry(
    rank = rank,
    userId = userId,
    username = username,
    avatarUrl = avatarUrl,
    color = color,
    cityName = cityName,
    clanTag = clanTag,
    totalAreaM2 = totalAreaM2,
    territoriesCount = territoriesCount,
    capturesCount = capturesCount,
    distanceWalkedM = distanceWalkedM
)

private fun ClanLeaderboardDto.toDomain() = ClanLeaderboardEntry(
    rank = rank,
    clanId = clanId,
    name = name,
    tag = tag,
    color = color,
    avatarUrl = avatarUrl,
    totalAreaM2 = totalAreaM2,
    membersCount = membersCount,
    territoriesCount = territoriesCount
)
