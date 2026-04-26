package com.territorywars.presentation.clan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.territorywars.domain.model.Clan
import com.territorywars.domain.model.ClanLeaderboardEntry
import com.territorywars.domain.model.ClanMember
import com.territorywars.domain.model.ClanRole
import com.territorywars.presentation.components.AppTextField
import com.territorywars.presentation.components.PrimaryButton
import com.territorywars.presentation.components.UserAvatar
import com.territorywars.presentation.map.AppBottomNav
import com.territorywars.presentation.map.AppSnackbar
import com.territorywars.presentation.theme.DmMono
import com.territorywars.presentation.theme.PlusJakartaSans
import com.territorywars.presentation.theme.parseColor

private fun formatArea(m2: Double): String = when {
    m2 < 1_000 -> "${m2.toInt()} м²"
    m2 < 1_000_000 -> "${"%.2f".format(m2 / 1_000_000)} км²"
    else -> "${"%.0f".format(m2 / 1_000_000)} км²"
}

private val CLAN_COLORS = listOf(
    "#E53935", "#FF6D00", "#FFD600", "#00C853",
    "#1DE9B6", "#2979FF", "#D500F9", "#FF4081",
    "#FF1744", "#6D4C41", "#1565C0", "#7C6EF8"
)

@Composable
fun ClanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    viewModel: ClanViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var requestTargetClan by remember { mutableStateOf<ClanLeaderboardEntry?>(null) }

    val clanAvatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadClanAvatar(it) } }

    val primary      = MaterialTheme.colorScheme.primary
    val bg           = MaterialTheme.colorScheme.background
    val surface      = MaterialTheme.colorScheme.surface
    val outlineVar   = MaterialTheme.colorScheme.outlineVariant
    val onBg         = MaterialTheme.colorScheme.onBackground
    val errorColor   = MaterialTheme.colorScheme.error
    val errorCont    = MaterialTheme.colorScheme.errorContainer
    val primaryCont  = MaterialTheme.colorScheme.primaryContainer

    var snackbarMsg   by remember { mutableStateOf<String?>(null) }
    var snackbarIsErr by remember { mutableStateOf(false) }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarMsg = it; snackbarIsErr = false
            viewModel.dismissSuccessMessage()
        }
    }
    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbarMsg = it; snackbarIsErr = true
            viewModel.dismissActionError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable {
                            if (state.isCreating) viewModel.hideCreateForm()
                            else onNavigateBack()
                        },
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад", tint = onBg, modifier = Modifier.size(18.dp))
                }
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
                Text(
                    text = if (state.isCreating) "Создать клан" else "Клан",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    color = onBg,
                )
            }
            HorizontalDivider(color = outlineVar)

            // ── Content ──────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = primary)
                        }
                    }
                    state.error != null -> {
                        ClanErrorState(message = state.error!!, onRetry = viewModel::loadData)
                    }
                    state.isCreating -> {
                        CreateClanForm(
                            form = state.createForm,
                            isLoading = state.isActionLoading,
                            primary = primary,
                            onBg = onBg,
                            onNameChanged = viewModel::onCreateNameChanged,
                            onTagChanged = viewModel::onCreateTagChanged,
                            onColorChanged = viewModel::onCreateColorChanged,
                            onDescriptionChanged = viewModel::onCreateDescriptionChanged,
                            onCreate = viewModel::createClan,
                        )
                    }
                    state.myClan != null -> {
                        MyClanContent(
                            clan = state.myClan!!,
                            members = state.members,
                            joinRequests = state.joinRequests,
                            activityItems = state.activityItems,
                            selectedTab = state.selectedTab,
                            isActivityLoading = state.isActivityLoading,
                            myUserId = state.myUserId,
                            isActionLoading = state.isActionLoading,
                            primary = primary,
                            bg = bg,
                            onLeaveClick = { showLeaveDialog = true },
                            onDeleteClick = { showDeleteDialog = true },
                            onKickMember = viewModel::kickMember,
                            onAcceptRequest = viewModel::acceptJoinRequest,
                            onDeclineRequest = viewModel::declineJoinRequest,
                            onTabSelected = viewModel::selectTab,
                            onAvatarUpload = { clanAvatarPicker.launch("image/*") },
                        )
                    }
                    else -> {
                        NoClanContent(
                            topClans = state.topClans,
                            isActionLoading = state.isActionLoading,
                            primary = primary,
                            onBg = onBg,
                            onCreateClan = viewModel::showCreateForm,
                            onClanTapped = { clan ->
                                requestTargetClan = clan
                                showRequestDialog = true
                            },
                        )
                    }
                }
            }

            // ── Bottom nav ───────────────────────────────────────────────────
            HorizontalDivider(color = outlineVar)
            AppBottomNav(
                active = "clan",
                onNavigate = { dest ->
                    when (dest) {
                        "map"         -> onNavigateToMap()
                        "profile"     -> onNavigateToProfile()
                        "leaderboard" -> onNavigateToLeaderboard()
                    }
                },
            )
        }

        // Snackbar
        snackbarMsg?.let { msg ->
            AppSnackbar(
                message = msg,
                onDismiss = { snackbarMsg = null },
                primary = primary,
                errorColor = if (snackbarIsErr) errorColor else primary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
            )
        }
    }

    // ── Leave dialog ─────────────────────────────────────────────────────────
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(errorCont),
                ) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = errorColor, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    "Покинуть клан?",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            text = {
                Text(
                    "Вы уверены, что хотите покинуть клан? Это действие необратимо.",
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showLeaveDialog = false; viewModel.leaveClan() },
                    colors = ButtonDefaults.textButtonColors(contentColor = errorColor),
                ) {
                    Text("Покинуть", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Отмена", fontFamily = PlusJakartaSans)
                }
            },
        )
    }

    // ── Join request dialog ───────────────────────────────────────────────────
    requestTargetClan?.let { clan ->
        if (showRequestDialog) {
            AlertDialog(
                onDismissRequest = { showRequestDialog = false; requestTargetClan = null },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
                icon = {
                    val color = remember(clan.color) { parseColor(clan.color) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color.copy(0.2f))
                            .border(2.dp, color, RoundedCornerShape(14.dp)),
                    ) {
                        Text(clan.tag.take(3), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = DmMono, color = color)
                    }
                },
                title = {
                    Text(
                        "Вступить в клан?",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                text = {
                    Text(
                        "Отправить заявку на вступление в клан «${clan.name}»?\nГлава клана должен её одобрить.",
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.requestJoinClan(clan.clanId)
                            showRequestDialog = false
                            requestTargetClan = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = primary),
                    ) {
                        Text("Да", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRequestDialog = false; requestTargetClan = null }) {
                        Text("Нет", fontFamily = PlusJakartaSans)
                    }
                },
            )
        }
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(errorCont),
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = errorColor, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    "Удалить клан?",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            text = {
                Text(
                    "Клан будет удалён навсегда. Все участники останутся без клана. Это действие необратимо.",
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.deleteClan() },
                    colors = ButtonDefaults.textButtonColors(contentColor = errorColor),
                ) {
                    Text("Удалить", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена", fontFamily = PlusJakartaSans)
                }
            },
        )
    }
}

// ── My clan ───────────────────────────────────────────────────────────────────

@Composable
private fun MyClanContent(
    clan: Clan,
    members: List<ClanMember>,
    joinRequests: List<ClanJoinRequestItem>,
    activityItems: List<ClanActivityItem>,
    selectedTab: ClanTab,
    isActivityLoading: Boolean,
    myUserId: String?,
    isActionLoading: Boolean,
    primary: Color,
    bg: Color,
    onLeaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onKickMember: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onTabSelected: (ClanTab) -> Unit,
    onAvatarUpload: () -> Unit,
) {
    val clanColor = remember(clan.color) { parseColor(clan.color) }
    val amILeader = members.any { it.userId == myUserId && it.role == ClanRole.LEADER }
    var acceptTarget by remember { mutableStateOf<ClanJoinRequestItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Gradient header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(clanColor.copy(alpha = 0.22f), bg)))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Clan avatar / tag box
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = clanColor, spotColor = clanColor)
                                .clip(RoundedCornerShape(16.dp))
                                .background(clanColor.copy(alpha = 0.2f))
                                .border(2.dp, clanColor, RoundedCornerShape(16.dp))
                                .then(if (amILeader) Modifier.clickable { onAvatarUpload() } else Modifier),
                        ) {
                            if (!clan.avatarUrl.isNullOrBlank()) {
                                val fullUrl = if (clan.avatarUrl!!.startsWith("http")) clan.avatarUrl
                                              else "http://93.183.74.141${clan.avatarUrl}"
                                AsyncImage(model = fullUrl, contentDescription = "Аватар клана",
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
                            } else {
                                Text(clan.tag, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = DmMono, color = clanColor)
                            }
                            if (amILeader) {
                                Box(Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(clanColor),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                        Column {
                            Text(clan.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurface)
                            if (!clan.description.isNullOrBlank()) {
                                Text(clan.description, fontSize = 13.sp, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ClanStatCard("Площадь", formatArea(clan.totalAreaM2), clanColor)
                        ClanStatCard("Участники", "${clan.membersCount}/${clan.maxMembers}", clanColor)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = primary,
            ) {
                listOf("Участники", "Топ", "История").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab.ordinal == index,
                        onClick = { onTabSelected(ClanTab.entries[index]) },
                        text = { Text(title, fontSize = 12.sp, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold) },
                    )
                }
            }
        }

        // Tab: Участники
        if (selectedTab == ClanTab.MEMBERS) {
            if (amILeader && joinRequests.isNotEmpty()) {
                item {
                    Text("ЗАЯВКИ (${joinRequests.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.error,
                        letterSpacing = 0.8.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                }
                items(joinRequests) { request ->
                    JoinRequestRow(request = request, primary = primary, onClick = { acceptTarget = request })
                }
                item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            }
            item {
                Text("УЧАСТНИКИ", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
            items(members.sortedBy { it.role.ordinal }) { member ->
                MemberRow(member = member, isMe = member.userId == myUserId,
                    canKick = amILeader && member.userId != myUserId && member.role != ClanRole.LEADER,
                    primary = primary, onKick = { onKickMember(member.userId) })
            }
            item {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!amILeader) {
                        OutlinedButton(onClick = onLeaveClick, enabled = !isActionLoading,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            if (isActionLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                            else { Icon(Icons.Outlined.ExitToApp, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Покинуть клан", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    if (amILeader) {
                        OutlinedButton(onClick = onDeleteClick, enabled = !isActionLoading,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            if (isActionLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                            else { Icon(Icons.Outlined.DeleteForever, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Удалить клан", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }

        // Tab: Топ
        if (selectedTab == ClanTab.TOP) {
            val sorted = members.sortedByDescending { it.totalAreaM2 }
            if (sorted.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Нет данных", fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                itemsIndexed(sorted) { index, member ->
                    TopMemberRow(rank = index + 1, member = member, isMe = member.userId == myUserId, primary = primary)
                }
            }
        }

        // Tab: История
        if (selectedTab == ClanTab.HISTORY) {
            if (isActivityLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primary)
                    }
                }
            } else if (activityItems.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Активности пока нет", fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(activityItems) { activity ->
                    ActivityRow(activity = activity, primary = primary)
                }
            }
        }
    }

    // Accept/decline dialog
    acceptTarget?.let { req ->
        AlertDialog(
            onDismissRequest = { acceptTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "Добавить игрока в клан?",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val color = remember(req.color) { parseColor(req.color) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(0.2f)),
                    ) {
                        Text(req.username.take(2).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = DmMono, color = color)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(req.username, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(formatArea(req.totalAreaM2), fontSize = 12.sp, fontFamily = DmMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAcceptRequest(req.userId); acceptTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = primary),
                ) {
                    Text("Да", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onDeclineRequest(req.userId); acceptTarget = null }) {
                    Text("Нет", fontFamily = PlusJakartaSans)
                }
            },
        )
    }
}

@Composable
private fun JoinRequestRow(
    request: ClanJoinRequestItem,
    primary: Color,
    onClick: () -> Unit,
) {
    val color = remember(request.color) { parseColor(request.color) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(username = request.username, avatarUrl = request.avatarUrl, color = color, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(request.username, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurface)
            Text(formatArea(request.totalAreaM2), fontSize = 11.sp, fontFamily = DmMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TopMemberRow(rank: Int, member: ClanMember, isMe: Boolean, primary: Color) {
    val color = remember(member.color) { parseColor(member.color) }
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isMe) primary.copy(alpha = 0.06f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("#$rank", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = DmMono, color = rankColor, modifier = Modifier.width(32.dp))
        UserAvatar(username = member.username, avatarUrl = member.avatarUrl, color = color, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(member.username + if (isMe) " (я)" else "", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(formatArea(member.totalAreaM2), fontSize = 13.sp, fontFamily = DmMono, fontWeight = FontWeight.Bold, color = primary)
    }
}

@Composable
private fun ActivityRow(activity: ClanActivityItem, primary: Color) {
    val color = remember(activity.ownerColor) { parseColor(activity.ownerColor) }
    val date = remember(activity.capturedAt) {
        try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val d = parser.parse(activity.capturedAt.take(19))
            if (d != null) java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(d)
            else activity.capturedAt.take(10)
        } catch (_: Exception) { activity.capturedAt.take(10) }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UserAvatar(username = activity.ownerUsername, avatarUrl = null, color = color, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(activity.ownerUsername, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurface)
            Text("захватил $date", fontSize = 11.sp, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatArea(activity.areaM2), fontSize = 13.sp, fontFamily = DmMono, fontWeight = FontWeight.Bold, color = primary)
    }
}

@Composable
private fun ClanStatCard(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = DmMono, color = accent)
        Text(label, fontSize = 11.sp, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MemberRow(
    member: ClanMember,
    isMe: Boolean,
    canKick: Boolean,
    primary: Color,
    onKick: () -> Unit,
) {
    val color = remember(member.color) { parseColor(member.color) }
    val roleLabel = when (member.role) {
        ClanRole.LEADER  -> "Лидер"
        ClanRole.OFFICER -> "Офицер"
        ClanRole.MEMBER  -> "Участник"
    }
    val roleColor = when (member.role) {
        ClanRole.LEADER  -> MaterialTheme.colorScheme.primary
        ClanRole.OFFICER -> MaterialTheme.colorScheme.tertiary
        ClanRole.MEMBER  -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val rowBg = if (isMe) primary.copy(alpha = 0.06f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(username = member.username, avatarUrl = member.avatarUrl, color = color, size = 40.dp)
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = member.username + if (isMe) " (я)" else "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(roleColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(roleLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, color = roleColor)
                }
            }
            Text(
                text = formatArea(member.totalAreaM2),
                fontSize = 11.sp,
                fontFamily = DmMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (canKick) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable(onClick = onKick),
            ) {
                Icon(Icons.Outlined.PersonRemove, contentDescription = "Исключить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
            }
        }
    }
}

// ── No clan ───────────────────────────────────────────────────────────────────

@Composable
private fun NoClanContent(
    topClans: List<ClanLeaderboardEntry>,
    isActionLoading: Boolean,
    primary: Color,
    onBg: Color,
    onCreateClan: () -> Unit,
    onClanTapped: (ClanLeaderboardEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            // Hero card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(primary.copy(0.16f), primary.copy(0.05f))))
                    .border(1.dp, primary.copy(0.3f), RoundedCornerShape(22.dp))
                    .padding(28.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(68.dp).clip(CircleShape).background(primary.copy(alpha = 0.15f)),
                    ) {
                        Icon(Icons.Outlined.Groups, contentDescription = null, tint = primary, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Вы не в клане",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        color = onBg,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Создайте свой клан или вступите\nв существующий",
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(
                        text = "Создать свой клан",
                        onClick = onCreateClan,
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }

        if (topClans.isNotEmpty()) {
            item {
                Text(
                    text = "ПОПУЛЯРНЫЕ КЛАНЫ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }

            itemsIndexed(topClans) { _, clan ->
                TopClanRow(
                    clan = clan,
                    isActionLoading = isActionLoading,
                    onJoin = { onClanTapped(clan) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f),
                )
            }
        }
    }
}

@Composable
private fun TopClanRow(
    clan: ClanLeaderboardEntry,
    isActionLoading: Boolean,
    onJoin: () -> Unit,
) {
    val color = remember(clan.color) { parseColor(clan.color) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(color.copy(0.2f))
                .border(1.dp, color.copy(0.5f), RoundedCornerShape(13.dp)),
        ) {
            Text(clan.tag.take(3), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = DmMono, color = color)
        }
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clan.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${clan.membersCount} участников • ${formatArea(clan.totalAreaM2)}",
                fontSize = 11.sp,
                fontFamily = DmMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(
            onClick = onJoin,
            enabled = !isActionLoading,
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text("Вступить", fontSize = 12.sp, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Create form ───────────────────────────────────────────────────────────────

@Composable
private fun CreateClanForm(
    form: CreateClanForm,
    isLoading: Boolean,
    primary: Color,
    onBg: Color,
    onNameChanged: (String) -> Unit,
    onTagChanged: (String) -> Unit,
    onColorChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCreate: () -> Unit,
) {
    val previewColor = remember(form.color) { parseColor(form.color) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Live preview card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(previewColor.copy(0.08f))
                    .border(1.5.dp, previewColor.copy(0.4f), RoundedCornerShape(16.dp))
                    .shadow(0.dp, RoundedCornerShape(16.dp), ambientColor = previewColor.copy(0.3f), spotColor = previewColor.copy(0.3f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor.copy(0.2f))
                        .border(1.5.dp, previewColor, RoundedCornerShape(12.dp)),
                ) {
                    Text(
                        text = form.tag.ifBlank { "TAG" }.take(4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = DmMono,
                        color = previewColor,
                    )
                }
                Column {
                    Text(
                        text = form.name.ifBlank { "Название клана" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = onBg,
                    )
                    Text(
                        "1 участник",
                        fontSize = 11.sp,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Clan name
        item {
            AppTextField(
                value = form.name,
                onValueChange = onNameChanged,
                label = "Название клана",
                leadingIcon = Icons.Outlined.Group,
                error = form.nameError,
                isValid = form.name.length >= 3 && form.nameError == null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }

        // Tag
        item {
            AppTextField(
                value = form.tag,
                onValueChange = onTagChanged,
                label = "Тег (2–4 символа)",
                leadingIcon = Icons.Outlined.Tag,
                error = form.tagError,
                isValid = form.tag.length >= 2 && form.tagError == null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            AnimatedVisibility(visible = form.tag.isNotBlank()) {
                Text(
                    text = "Отображается рядом с именем: [${form.tag}]",
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
        }

        // Color picker
        item {
            Text(
                "Цвет клана",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(10.dp))
            ClanColorPicker(selectedHex = form.color, onSelected = onColorChanged)
        }

        // Description
        item {
            OutlinedTextField(
                value = form.description,
                onValueChange = onDescriptionChanged,
                label = { Text("Описание (необязательно)", fontFamily = PlusJakartaSans) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                textStyle = LocalTextStyle.current.copy(fontFamily = PlusJakartaSans),
                supportingText = {
                    Text(
                        "${form.description.length}/200",
                        fontFamily = DmMono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        item {
            PrimaryButton(
                text = "Создать клан",
                onClick = onCreate,
                isLoading = isLoading,
                enabled = form.isValid && !isLoading,
            )
        }
    }
}

@Composable
private fun ClanColorPicker(selectedHex: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CLAN_COLORS.forEach { hex ->
            val color = remember(hex) { parseColor(hex) }
            val isSelected = hex.equals(selectedHex, ignoreCase = true)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = CircleShape,
                    )
                    .clickable { onSelected(hex) },
            ) {
                if (isSelected) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun ClanErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                Text("Повторить", fontFamily = PlusJakartaSans)
            }
        }
    }
}
