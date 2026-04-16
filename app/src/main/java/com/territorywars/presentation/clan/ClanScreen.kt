package com.territorywars.presentation.clan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.domain.model.Clan
import com.territorywars.domain.model.ClanLeaderboardEntry
import com.territorywars.domain.model.ClanMember
import com.territorywars.domain.model.ClanRole
import com.territorywars.presentation.components.AppTextField
import com.territorywars.presentation.components.PrimaryButton
import com.territorywars.presentation.theme.Primary
import com.territorywars.presentation.theme.Tertiary

private fun formatArea(m2: Double): String = when {
    m2 < 1_000 -> "${m2.toInt()} м²"
    m2 < 100_000 -> "${"%.2f".format(m2 / 10_000)} га"
    else -> "${"%.0f".format(m2 / 10_000)} га"
}

private val CLAN_COLORS = listOf(
    "#E53935", "#FF6D00", "#FFD600", "#00C853",
    "#1DE9B6", "#2979FF", "#D500F9", "#FF4081",
    "#FF1744", "#6D4C41", "#1565C0", "#FF6D00"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClanScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClanViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLeaveDialog by remember { mutableStateOf(false) }

    // Snackbar для успехов и ошибок
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSuccessMessage()
        }
    }
    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissActionError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isCreating) "Создать клан" else "Клан")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isCreating) viewModel.hideCreateForm()
                        else onNavigateBack()
                    }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    ErrorState(message = state.error!!, onRetry = viewModel::loadData)
                }
                state.isCreating -> {
                    CreateClanForm(
                        form = state.createForm,
                        isLoading = state.isActionLoading,
                        onNameChanged = viewModel::onCreateNameChanged,
                        onTagChanged = viewModel::onCreateTagChanged,
                        onColorChanged = viewModel::onCreateColorChanged,
                        onDescriptionChanged = viewModel::onCreateDescriptionChanged,
                        onCreate = viewModel::createClan
                    )
                }
                state.myClan != null -> {
                    MyClanContent(
                        clan = state.myClan!!,
                        members = state.members,
                        myUserId = state.myUserId,
                        isActionLoading = state.isActionLoading,
                        onLeaveClick = { showLeaveDialog = true },
                        onKickMember = viewModel::kickMember
                    )
                }
                else -> {
                    NoClanContent(
                        topClans = state.topClans,
                        isActionLoading = state.isActionLoading,
                        onCreateClan = viewModel::showCreateForm,
                        onJoinClan = viewModel::joinClan
                    )
                }
            }
        }
    }

    // Диалог подтверждения выхода из клана
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Покинуть клан?") },
            text = { Text("Вы уверены, что хотите покинуть клан? Это действие необратимо.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        viewModel.leaveClan()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Покинуть")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Отмена") }
            }
        )
    }
}

// ====== Экран "Я в клане" ======

@Composable
private fun MyClanContent(
    clan: Clan,
    members: List<ClanMember>,
    myUserId: String?,
    isActionLoading: Boolean,
    onLeaveClick: () -> Unit,
    onKickMember: (String) -> Unit
) {
    val clanColor = remember(clan.color) {
        try { Color(android.graphics.Color.parseColor(clan.color).toLong() or 0xFF000000L) }
        catch (_: Exception) { Primary }
    }
    val amILeader = members.any { it.userId == myUserId && it.role == ClanRole.LEADER }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Заголовок клана
        item {
            ClanHeader(clan = clan, clanColor = clanColor, membersCount = members.size)
        }

        // Список участников
        item {
            Text(
                text = "Участники (${members.size}/${clan.maxMembers})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(members.sortedBy { it.role.ordinal }) { member ->
            MemberRow(
                member = member,
                isMe = member.userId == myUserId,
                canKick = amILeader && member.userId != myUserId && member.role != ClanRole.LEADER,
                onKick = { onKickMember(member.userId) }
            )
        }

        // Кнопка выйти
        item {
            Spacer(modifier = Modifier.height(16.dp))
            if (!amILeader) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    OutlinedButton(
                        onClick = onLeaveClick,
                        enabled = !isActionLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        if (isActionLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Покинуть клан")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClanHeader(clan: Clan, clanColor: Color, membersCount: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Цветной блок клана
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(clanColor.copy(alpha = 0.25f))
                        .border(2.dp, clanColor, RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = clan.tag,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = clanColor
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = clan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$membersCount/${clan.maxMembers} участников",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!clan.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = clan.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                ClanStat("Площадь", formatArea(clan.totalAreaM2))
                ClanStat("Участники", "$membersCount")
            }
        }
    }
}

@Composable
private fun ClanStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MemberRow(
    member: ClanMember,
    isMe: Boolean,
    canKick: Boolean,
    onKick: () -> Unit
) {
    val color = remember(member.color) {
        try { Color(android.graphics.Color.parseColor(member.color).toLong() or 0xFF000000L) }
        catch (_: Exception) { Primary }
    }
    val roleLabel = when (member.role) {
        ClanRole.LEADER  -> "Лидер"
        ClanRole.OFFICER -> "Офицер"
        ClanRole.MEMBER  -> "Участник"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
        ) {
            Text(
                text = member.username.take(2).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.username + if (isMe) " (я)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (member.role) {
                        ClanRole.LEADER  -> Primary.copy(alpha = 0.15f)
                        ClanRole.OFFICER -> Tertiary.copy(alpha = 0.15f)
                        ClanRole.MEMBER  -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (member.role) {
                            ClanRole.LEADER  -> Primary
                            ClanRole.OFFICER -> Tertiary
                            ClanRole.MEMBER  -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = formatArea(member.totalAreaM2),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (canKick) {
            IconButton(onClick = onKick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.PersonRemove,
                    contentDescription = "Исключить",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ====== Экран "Нет клана" ======

@Composable
private fun NoClanContent(
    topClans: List<ClanLeaderboardEntry>,
    isActionLoading: Boolean,
    onCreateClan: () -> Unit,
    onJoinClan: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Кнопка создания
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Primary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Groups, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Вы не состоите в клане", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Создайте свой клан или вступите в существующий",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCreateClan,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Создать клан")
                        }
                    }
                }
            }
        }

        if (topClans.isNotEmpty()) {
            item {
                Text(
                    text = "Топ кланов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            itemsIndexed(topClans) { _, clan ->
                TopClanRow(
                    clan = clan,
                    isActionLoading = isActionLoading,
                    onJoin = { onJoinClan(clan.clanId) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun TopClanRow(
    clan: ClanLeaderboardEntry,
    isActionLoading: Boolean,
    onJoin: () -> Unit
) {
    val color = remember(clan.color) {
        try { Color(android.graphics.Color.parseColor(clan.color).toLong() or 0xFF000000L) }
        catch (_: Exception) { Primary }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Text(
                text = clan.tag.take(3),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clan.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${clan.membersCount} участников • ${formatArea(clan.totalAreaM2)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onJoin,
            enabled = !isActionLoading,
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Вступить", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ====== Форма создания клана ======

@Composable
private fun CreateClanForm(
    form: CreateClanForm,
    isLoading: Boolean,
    onNameChanged: (String) -> Unit,
    onTagChanged: (String) -> Unit,
    onColorChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCreate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AppTextField(
                value = form.name,
                onValueChange = onNameChanged,
                label = "Название клана",
                leadingIcon = Icons.Outlined.Group,
                error = form.nameError,
                isValid = form.name.length >= 3 && form.nameError == null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        item {
            AppTextField(
                value = form.tag,
                onValueChange = onTagChanged,
                label = "Тег (2–4 символа)",
                leadingIcon = Icons.Outlined.Tag,
                error = form.tagError,
                isValid = form.tag.length >= 2 && form.tagError == null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Text(
                text = "Отображается рядом с именем: [${form.tag.ifBlank { "TAG" }}]",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        item {
            Text(
                text = "Цвет клана",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            ClanColorPicker(selectedHex = form.color, onSelected = onColorChanged)
        }

        item {
            OutlinedTextField(
                value = form.description,
                onValueChange = onDescriptionChanged,
                label = { Text("Описание (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3,
                supportingText = { Text("${form.description.length}/200") }
            )
        }

        item {
            PrimaryButton(
                text = "Создать клан",
                onClick = onCreate,
                isLoading = isLoading,
                enabled = form.isValid && !isLoading
            )
        }
    }
}

@Composable
private fun ClanColorPicker(selectedHex: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CLAN_COLORS.forEach { hex ->
            val color = remember(hex) {
                try { Color(android.graphics.Color.parseColor(hex).toLong() or 0xFF000000L) }
                catch (_: Exception) { Primary }
            }
            val isSelected = hex.equals(selectedHex, ignoreCase = true)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = CircleShape
                    )
                    .clickable { onSelected(hex) }
            ) {
                if (isSelected) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ====== Общие компоненты ======

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) { Text("Повторить") }
        }
    }
}
