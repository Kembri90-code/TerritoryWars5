package com.territorywars.presentation.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.domain.model.ClanLeaderboardEntry
import com.territorywars.domain.model.PlayerLeaderboardEntry
import com.territorywars.presentation.components.UserAvatar
import com.territorywars.presentation.map.AppBottomNav
import com.territorywars.presentation.theme.DmMono
import com.territorywars.presentation.theme.PlusJakartaSans
import com.territorywars.presentation.theme.parseColor

private fun formatArea(m2: Double): String = when {
    m2 < 1_000     -> "${m2.toInt()} м²"
    m2 < 1_000_000 -> "${"%.2f".format(m2 / 1_000_000)} км²"
    else           -> "${"%.0f".format(m2 / 1_000_000)} км²"
}

@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToClan: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val primary  = MaterialTheme.colorScheme.primary
    val bg       = MaterialTheme.colorScheme.background
    val surface  = MaterialTheme.colorScheme.surface
    val outline  = MaterialTheme.colorScheme.outline
    val outlineVar = MaterialTheme.colorScheme.outlineVariant
    val onBg     = MaterialTheme.colorScheme.onBackground
    val onSurfVar = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Рейтинг",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    color = onBg,
                    modifier = Modifier.weight(1f),
                )
            }

            // Tabs
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Игроки" to 0, "Кланы" to 1).forEach { (label, idx) ->
                    val selected = state.selectedTab == idx
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.onTabSelected(idx) }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            color = if (selected) primary else onSurfVar,
                        )
                    }
                }
            }
            // Tab indicator
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(2) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .background(if (state.selectedTab == idx) primary else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = outlineVar)

        // ── Sort chips (players only) ─────────────────────────────────────────
        if (state.selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlayerSort.entries.forEach { sort ->
                    val selected = state.playerSort == sort
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) primary.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.5.dp, if (selected) primary.copy(alpha = 0.7f) else outline, CircleShape)
                            .then(
                                Modifier.clickableNoRipple { viewModel.onPlayerSortChanged(sort) }
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = sort.label,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) primary else onSurfVar,
                        )
                    }
                }
            }
            HorizontalDivider(color = outlineVar)
        }

        // ── Content ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (state.selectedTab) {
                0 -> PlayersContent(state = state, onTabClick = viewModel::onTabSelected)
                1 -> ClansContent(state = state, onTabClick = viewModel::onTabSelected)
            }
        }

        AppBottomNav(
            active = "leaderboard",
            onNavigate = { id ->
                when (id) {
                    "map"     -> onNavigateToMap()
                    "profile" -> onNavigateToProfile()
                    "clan"    -> onNavigateToClan()
                }
            },
        )
    }
}

// Extension to avoid ripple on chips
private fun Modifier.clickableNoRipple(onClick: () -> Unit) =
    this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
            onClick = onClick,
        )
    )

// ── Players tab ───────────────────────────────────────────────────────────────

@Composable
private fun PlayersContent(state: LeaderboardState, onTabClick: (Int) -> Unit) {
    when {
        state.isLoadingPlayers -> LoadingState()
        state.playersError != null -> ErrorState(state.playersError) { onTabClick(0) }
        state.players.isEmpty() -> EmptyState("Рейтинг пока пуст")
        else -> {
            val primary  = MaterialTheme.colorScheme.primary
            val bg       = MaterialTheme.colorScheme.background
            val surfCont = MaterialTheme.colorScheme.surfaceVariant
            val outlineVar = MaterialTheme.colorScheme.outlineVariant

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                // Podium (top 3)
                if (state.players.size >= 3) {
                    item {
                        Podium(players = state.players, primary = primary, bg = bg)
                    }
                }

                // Rows 4+
                itemsIndexed(state.players.drop(3)) { idx, player ->
                    PlayerRow(player = player, rank = idx + 4, sort = state.playerSort)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = outlineVar.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Podium(players: List<PlayerLeaderboardEntry>, primary: Color, bg: Color) {
    val surfCont = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(primary.copy(alpha = 0.12f), bg)
                )
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        // Order: 2nd (left), 1st (center), 3rd (right)
        val podiumOrder = listOf(1, 0, 2)
        val podiumHeights = listOf(80.dp, 104.dp, 64.dp)
        val medals = listOf("🥇", "🥈", "🥉")
        val podiumGradients = listOf(
            Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFF59E0B))),
            Brush.verticalGradient(listOf(Color(0xFFC0C0C0), Color(0xFF9CA3AF))),
            Brush.verticalGradient(listOf(Color(0xFFCD7F32), Color(0xFF92400E))),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            podiumOrder.forEachIndexed { col, rank ->
                val player = players.getOrNull(rank) ?: return@forEachIndexed
                val playerColor = remember(player.color) { parseColor(player.color) }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    if (rank == 0) Text("👑", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Avatar
                    UserAvatar(
                        username = player.username,
                        avatarUrl = player.avatarUrl,
                        color = playerColor,
                        size = if (rank == 0) 46.dp else 36.dp,
                        borderModifier = Modifier.border(1.5.dp, playerColor.copy(alpha = 0.5f), CircleShape),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = player.username,
                        fontSize = if (rank == 0) 13.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatArea(player.totalAreaM2),
                        fontSize = 11.sp,
                        fontFamily = DmMono,
                        fontWeight = FontWeight.Bold,
                        color = playerColor,
                    )
                    // Podium block
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(podiumHeights[col])
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(podiumGradients[col])
                            .padding(top = 8.dp),
                    ) {
                        Text(medals[rank], fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(player: PlayerLeaderboardEntry, rank: Int, sort: PlayerSort) {
    val playerColor = remember(player.color) { parseColor(player.color) }
    val primary     = MaterialTheme.colorScheme.primary
    val onBg        = MaterialTheme.colorScheme.onBackground
    val onSurfVar   = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVar  = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "$rank",
            fontSize = 13.sp,
            fontFamily = DmMono,
            fontWeight = FontWeight.SemiBold,
            color = onSurfVar,
            modifier = Modifier.width(24.dp),
        )
        UserAvatar(username = player.username, avatarUrl = player.avatarUrl, color = playerColor, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = player.username,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = onBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (player.clanTag != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "[${player.clanTag}]",
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                        color = onSurfVar,
                    )
                }
            }
            if (player.cityName != null) {
                Text(
                    text = player.cityName,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                    color = onSurfVar,
                )
            }
        }
        val value = when (sort) {
            PlayerSort.AREA     -> formatArea(player.totalAreaM2)
            PlayerSort.CAPTURES -> "${player.capturesCount} захв."
            PlayerSort.DISTANCE -> formatDistance(player.distanceWalkedM)
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = DmMono,
            fontWeight = FontWeight.Bold,
            color = primary,
        )
    }
}

private fun formatDistance(m: Double): String = when {
    m < 1_000 -> "${m.toInt()} м"
    else      -> "${"%.1f".format(m / 1_000)} км"
}

// ── Clans tab ─────────────────────────────────────────────────────────────────

@Composable
private fun ClansContent(state: LeaderboardState, onTabClick: (Int) -> Unit) {
    when {
        state.isLoadingClans -> LoadingState()
        state.clansError != null -> ErrorState(state.clansError) { onTabClick(1) }
        state.clans.isEmpty() -> EmptyState("Кланов пока нет")
        else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)) {
            itemsIndexed(state.clans) { idx, clan ->
                ClanRow(clan = clan, rank = idx + 1)
            }
        }
    }
}

@Composable
private fun ClanRow(clan: ClanLeaderboardEntry, rank: Int) {
    val clanColor  = remember(clan.color) { parseColor(clan.color) }
    val primary    = MaterialTheme.colorScheme.primary
    val bg         = MaterialTheme.colorScheme.background
    val surfCont   = MaterialTheme.colorScheme.surfaceContainerLow
    val outline    = MaterialTheme.colorScheme.outline
    val outlineVar = MaterialTheme.colorScheme.outlineVariant
    val onBg       = MaterialTheme.colorScheme.onBackground
    val onSurfVar  = MaterialTheme.colorScheme.onSurfaceVariant
    val medals     = listOf("🥇", "🥈", "🥉")
    val isTop      = rank <= 3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isTop) surfCont else bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (isTop) medals[rank - 1] else "$rank",
            fontSize = if (isTop) 18.sp else 13.sp,
            fontFamily = DmMono,
            color = onSurfVar,
            modifier = Modifier.width(24.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(clanColor.copy(alpha = if (MaterialTheme.colorScheme.background == Color(0xFF0C0C15)) 0.18f else 0.12f))
                .border(1.5.dp, clanColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        ) {
            if (!clan.avatarUrl.isNullOrBlank()) {
                val fullUrl = if (clan.avatarUrl!!.startsWith("http")) clan.avatarUrl
                              else "http://93.183.74.141${clan.avatarUrl}"
                AsyncImage(model = fullUrl, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
            } else {
                Text(text = clan.tag.take(4), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans, color = clanColor, letterSpacing = 0.5.sp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clan.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = onBg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${clan.membersCount} участников · ${clan.territoriesCount} территорий",
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                color = onSurfVar,
            )
        }
        Text(
            text = formatArea(clan.totalAreaM2),
            fontSize = 13.sp,
            fontFamily = DmMono,
            fontWeight = FontWeight.Bold,
            color = primary,
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = outlineVar.copy(alpha = 0.3f),
    )
}

// ── Shared states ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Outlined.CloudOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) { Text("Повторить") }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
