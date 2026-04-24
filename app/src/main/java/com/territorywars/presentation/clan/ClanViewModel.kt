package com.territorywars.presentation.clan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territorywars.data.remote.api.ClanApi
import com.territorywars.data.remote.api.ClanActivityDto
import com.territorywars.data.remote.api.ClanDto
import com.territorywars.data.remote.api.ClanJoinRequestDto
import com.territorywars.data.remote.api.ClanMemberDto
import com.territorywars.data.remote.api.CreateClanRequest
import com.territorywars.data.remote.api.LeaderboardApi
import com.territorywars.data.remote.api.ClanLeaderboardDto
import com.territorywars.data.remote.api.UserApi
import com.territorywars.domain.model.Clan
import com.territorywars.domain.model.ClanLeaderboardEntry
import com.territorywars.domain.model.ClanMember
import com.territorywars.domain.model.ClanRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClanJoinRequestItem(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val color: String,
    val totalAreaM2: Double,
    val createdAt: String
)

data class ClanActivityItem(
    val territoryId: String,
    val ownerId: String,
    val ownerUsername: String,
    val ownerColor: String,
    val areaM2: Double,
    val capturedAt: String
)

enum class ClanTab { MEMBERS, TOP, HISTORY }

// ---- Форма создания клана ----
data class CreateClanForm(
    val name: String = "",
    val tag: String = "",
    val color: String = "#2979FF",
    val description: String = "",
    val nameError: String? = null,
    val tagError: String? = null
) {
    val isValid: Boolean
        get() = name.length >= 3 && tag.length in 2..4 && nameError == null && tagError == null
}

// ---- Состояние экрана клана ----
data class ClanState(
    val myClan: Clan? = null,
    val myUserId: String? = null,
    val members: List<ClanMember> = emptyList(),
    val joinRequests: List<ClanJoinRequestItem> = emptyList(),
    val activityItems: List<ClanActivityItem> = emptyList(),
    val selectedTab: ClanTab = ClanTab.MEMBERS,
    val topClans: List<ClanLeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isActivityLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val error: String? = null,
    val actionError: String? = null,
    val successMessage: String? = null,
    val isCreating: Boolean = false,
    val createForm: CreateClanForm = CreateClanForm()
)

@HiltViewModel
class ClanViewModel @Inject constructor(
    private val userApi: UserApi,
    private val clanApi: ClanApi,
    private val leaderboardApi: LeaderboardApi
) : ViewModel() {

    private val _state = MutableStateFlow(ClanState())
    val state: StateFlow<ClanState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val userResponse = userApi.getMe()
                if (!userResponse.isSuccessful) {
                    _state.update { it.copy(isLoading = false, error = "Не удалось загрузить данные") }
                    return@launch
                }
                val user = userResponse.body()!!
                _state.update { it.copy(myUserId = user.id) }

                if (user.clanId != null) {
                    // Загружаем клан и участников
                    val clanResponse = clanApi.getClanById(user.clanId)
                    val membersResponse = clanApi.getClanMembers(user.clanId)
                    if (clanResponse.isSuccessful && membersResponse.isSuccessful) {
                        val clan = clanResponse.body()!!.toDomain()
                        val members = membersResponse.body()!!.map { m -> m.toDomain() }
                        // Загружаем заявки только если пользователь — лидер
                        val requests = if (clan.leaderId == user.id) {
                            val r = clanApi.getClanRequests(user.clanId)
                            if (r.isSuccessful) r.body()!!.map { it.toItem() } else emptyList()
                        } else emptyList()
                        _state.update {
                            it.copy(
                                myClan = clan,
                                members = members,
                                joinRequests = requests,
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Ошибка загрузки клана") }
                    }
                } else {
                    // Загружаем топ кланов
                    val topResponse = leaderboardApi.getClansLeaderboard(limit = 20)
                    _state.update {
                        it.copy(
                            myClan = null,
                            topClans = if (topResponse.isSuccessful)
                                topResponse.body()!!.map { dto -> dto.toDomain() }
                            else emptyList(),
                            isLoading = false
                        )
                    }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, error = "Нет подключения к серверу") }
            }
        }
    }

    // ---- Создание клана ----

    fun showCreateForm() {
        _state.update { it.copy(isCreating = true, createForm = CreateClanForm(), actionError = null) }
    }

    fun hideCreateForm() {
        _state.update { it.copy(isCreating = false, createForm = CreateClanForm()) }
    }

    fun onCreateNameChanged(value: String) {
        val error = when {
            value.length < 3 -> "Минимум 3 символа"
            value.length > 30 -> "Максимум 30 символов"
            !value.matches(Regex("^[\\w\\sА-Яа-яёЁ]+$")) -> "Недопустимые символы"
            else -> null
        }
        _state.update { it.copy(createForm = it.createForm.copy(name = value, nameError = error)) }
    }

    fun onCreateTagChanged(value: String) {
        val upper = value.uppercase().filter { it.isLetterOrDigit() }.take(4)
        val error = when {
            upper.length < 2 -> "Минимум 2 символа"
            else -> null
        }
        _state.update { it.copy(createForm = it.createForm.copy(tag = upper, tagError = error)) }
    }

    fun onCreateColorChanged(hex: String) {
        _state.update { it.copy(createForm = it.createForm.copy(color = hex)) }
    }

    fun onCreateDescriptionChanged(value: String) {
        _state.update { it.copy(createForm = it.createForm.copy(description = value.take(200))) }
    }

    fun createClan() {
        val form = _state.value.createForm
        if (!form.isValid) return

        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                val response = clanApi.createClan(
                    CreateClanRequest(
                        name = form.name.trim(),
                        tag = form.tag,
                        color = form.color,
                        description = form.description.trim().ifBlank { null }
                    )
                )
                if (response.isSuccessful) {
                    _state.update {
                        it.copy(
                            isActionLoading = false,
                            isCreating = false,
                            myClan = response.body()!!.toDomain(),
                            members = emptyList(),
                            successMessage = "✓ Клан «${form.name}» создан!"
                        )
                    }
                    // Загружаем участников
                    val clanId = response.body()!!.id
                    val membersResponse = clanApi.getClanMembers(clanId)
                    if (membersResponse.isSuccessful) {
                        _state.update { it.copy(members = membersResponse.body()!!.map { m -> m.toDomain() }) }
                    }
                } else {
                    val msg = when (response.code()) {
                        409 -> "Клан с таким именем или тегом уже существует"
                        else -> "Ошибка создания клана"
                    }
                    _state.update { it.copy(isActionLoading = false, actionError = msg) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isActionLoading = false, actionError = "Нет подключения к серверу") }
            }
        }
    }

    // ---- Вступление в клан ----

    fun joinClan(clanId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                val response = clanApi.joinClan(clanId)
                if (response.isSuccessful) {
                    loadData()
                } else {
                    val msg = when (response.code()) {
                        403 -> "Клан закрыт для вступления"
                        409 -> "Вы уже в клане"
                        else -> "Не удалось вступить в клан"
                    }
                    _state.update { it.copy(isActionLoading = false, actionError = msg) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isActionLoading = false, actionError = "Нет подключения к серверу") }
            }
        }
    }

    // ---- Выход из клана ----

    fun leaveClan() {
        val clanId = _state.value.myClan?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                val response = clanApi.leaveClan(clanId)
                if (response.isSuccessful) {
                    _state.update {
                        it.copy(
                            isActionLoading = false,
                            myClan = null,
                            members = emptyList(),
                            successMessage = "✓ Вы покинули клан"
                        )
                    }
                    loadData()
                } else {
                    _state.update { it.copy(isActionLoading = false, actionError = "Не удалось покинуть клан") }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isActionLoading = false, actionError = "Нет подключения к серверу") }
            }
        }
    }

    // ---- Удаление клана (только для лидера) ----

    fun deleteClan() {
        val clanId = _state.value.myClan?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                val response = clanApi.deleteClan(clanId)
                if (response.isSuccessful) {
                    _state.update {
                        it.copy(
                            isActionLoading = false,
                            myClan = null,
                            members = emptyList(),
                            successMessage = "✓ Клан удалён"
                        )
                    }
                    loadData()
                } else {
                    val msg = when (response.code()) {
                        403 -> "Только лидер может удалить клан"
                        else -> "Ошибка удаления клана"
                    }
                    _state.update { it.copy(isActionLoading = false, actionError = msg) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isActionLoading = false, actionError = "Нет подключения к серверу") }
            }
        }
    }

    // ---- Кик участника (только для лидера) ----

    fun kickMember(userId: String) {
        val clanId = _state.value.myClan?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true) }
            try {
                clanApi.kickMember(clanId, userId)
                _state.update {
                    it.copy(
                        isActionLoading = false,
                        members = it.members.filter { m -> m.userId != userId }
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(isActionLoading = false, actionError = "Не удалось исключить участника") }
            }
        }
    }

    // ---- Заявка на вступление в клан ----

    fun requestJoinClan(clanId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                val response = clanApi.requestJoinClan(clanId)
                if (response.isSuccessful) {
                    _state.update {
                        it.copy(isActionLoading = false, successMessage = "✓ Заявка отправлена главе клана")
                    }
                } else {
                    val msg = when (response.code()) {
                        409 -> "Заявка уже отправлена или вы уже в клане"
                        403 -> "Клан заполнен"
                        else -> "Не удалось отправить заявку"
                    }
                    _state.update { it.copy(isActionLoading = false, actionError = msg) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isActionLoading = false, actionError = "Нет подключения к серверу") }
            }
        }
    }

    // ---- Принять заявку (лидер) ----

    fun acceptJoinRequest(userId: String) {
        val clanId = _state.value.myClan?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true) }
            try {
                val response = clanApi.acceptJoinRequest(clanId, userId)
                if (response.isSuccessful) {
                    _state.update {
                        it.copy(
                            isActionLoading = false,
                            joinRequests = it.joinRequests.filter { r -> r.userId != userId },
                            successMessage = "✓ Участник добавлен в клан"
                        )
                    }
                    loadData()
                } else {
                    _state.update { it.copy(isActionLoading = false, actionError = "Не удалось принять заявку") }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isActionLoading = false, actionError = "Нет подключения к серверу") }
            }
        }
    }

    // ---- Отклонить заявку (лидер) ----

    fun declineJoinRequest(userId: String) {
        val clanId = _state.value.myClan?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true) }
            try {
                clanApi.declineJoinRequest(clanId, userId)
                _state.update {
                    it.copy(
                        isActionLoading = false,
                        joinRequests = it.joinRequests.filter { r -> r.userId != userId }
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(isActionLoading = false, actionError = "Нет подключения к серверу") }
            }
        }
    }

    fun selectTab(tab: ClanTab) {
        _state.update { it.copy(selectedTab = tab) }
        if (tab == ClanTab.HISTORY && _state.value.activityItems.isEmpty()) {
            loadActivity()
        }
    }

    fun loadActivity() {
        val clanId = _state.value.myClan?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActivityLoading = true) }
            try {
                val resp = clanApi.getClanActivity(clanId)
                if (resp.isSuccessful) {
                    _state.update {
                        it.copy(
                            isActivityLoading = false,
                            activityItems = resp.body()!!.map { dto -> dto.toItem() }
                        )
                    }
                } else {
                    _state.update { it.copy(isActivityLoading = false) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isActivityLoading = false) }
            }
        }
    }

    fun dismissSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    fun dismissActionError() {
        _state.update { it.copy(actionError = null) }
    }
}

// ---- Маппинг DTO → Domain ----

private fun ClanDto.toDomain() = Clan(
    id = id, name = name, tag = tag, leaderId = leaderId, color = color,
    description = description, totalAreaM2 = totalAreaM2,
    membersCount = membersCount, maxMembers = maxMembers, createdAt = createdAt
)

private fun ClanMemberDto.toDomain() = ClanMember(
    userId = userId,
    username = username,
    avatarUrl = avatarUrl,
    color = color,
    role = when (role.uppercase()) {
        "LEADER"  -> ClanRole.LEADER
        "OFFICER" -> ClanRole.OFFICER
        else      -> ClanRole.MEMBER
    },
    totalAreaM2 = totalAreaM2,
    joinedAt = joinedAt
)

private fun ClanJoinRequestDto.toItem() = ClanJoinRequestItem(
    userId = userId, username = username, avatarUrl = avatarUrl,
    color = color, totalAreaM2 = totalAreaM2, createdAt = createdAt
)

private fun ClanLeaderboardDto.toDomain() = ClanLeaderboardEntry(
    rank = rank, clanId = clanId, name = name, tag = tag, color = color,
    totalAreaM2 = totalAreaM2, membersCount = membersCount, territoriesCount = territoriesCount
)

private fun ClanActivityDto.toItem() = ClanActivityItem(
    territoryId = territoryId, ownerId = ownerId,
    ownerUsername = ownerUsername, ownerColor = ownerColor,
    areaM2 = areaM2, capturedAt = capturedAt
)
