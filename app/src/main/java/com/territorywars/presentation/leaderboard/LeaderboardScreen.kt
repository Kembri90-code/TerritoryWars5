package com.territorywars.presentation.leaderboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.domain.model.ClanLeaderboardEntry
import com.territorywars.domain.model.PlayerLeaderboardEntry
import com.territorywars.presentation.theme.Primary
import com.territorywars.presentation.theme.Tertiary

private fun formatArea(m2: Double): String = when {
    m2 < 1_000 -> "${m2.toInt()} м²"
    m2 < 100_000 -> "${"%.2f".format(m2 / 10_000)} га"
    else -> "${"%.0f".format(m2 / 10_000)} га"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рейтинг") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Вкладки Игроки / Кланы
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Игроки") },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("Кланы") },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (state.selectedTab) {
                0 -> PlayersTab(state = state, onSortChanged = viewModel::onPlayerSortChanged, onRetry = { viewModel.loadPlayers() })
                1 -> ClansTab(state = state, onRetry = viewModel::loadClans)
            }
        }
    }
}

@Composable
private fun PlayersTab(
    state: LeaderboardState,
    onSortChanged: (PlayerSort) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Сортировка
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerSort.entries.forEach { sort ->
                FilterChip(
                    selected = state.playerSort == sort,
                    onClick = { onSortChanged(sort) },
                    label = { Text(sort.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        when {
            state.isLoadingPlayers -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.playersError != null -> {
                LeaderboardError(message = state.playersError, onRetry = onRetry)
            }
            state.players.isEmpty() -> {
                EmptyState("Рейтинг пока пуст")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(state.players) { _, player ->
                        PlayerRow(player = player)
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(player: PlayerLeaderboardEntry) {
    val color = remember(player.color) {
        try { Color(android.graphics.Color.parseColor(player.color).toLong() or 0xFF000000L) }
        catch (_: Exception) { Primary }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Место
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(36.dp)
        ) {
            when (player.rank) {
                1 -> Text("🥇", style = MaterialTheme.typography.titleMedium)
                2 -> Text("🥈", style = MaterialTheme.typography.titleMedium)
                3 -> Text("🥉", style = MaterialTheme.typography.titleMedium)
                else -> Text(
                    text = "${player.rank}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Цветовой кружок (аватар)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
        ) {
            Text(
                text = player.username.take(2).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Имя + клан + город
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = player.username,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (player.clanTag != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "[${player.clanTag}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (player.cityName != null) {
                Text(
                    text = player.cityName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Площадь
        Text(
            text = formatArea(player.totalAreaM2),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}

@Composable
private fun ClansTab(
    state: LeaderboardState,
    onRetry: () -> Unit
) {
    when {
        state.isLoadingClans -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.clansError != null -> {
            LeaderboardError(message = state.clansError, onRetry = onRetry)
        }
        state.clans.isEmpty() -> {
            EmptyState("Кланов пока нет")
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                itemsIndexed(state.clans) { _, clan ->
                    ClanRow(clan = clan)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClanRow(clan: ClanLeaderboardEntry) {
    val color = remember(clan.color) {
        try { Color(android.graphics.Color.parseColor(clan.color).toLong() or 0xFF000000L) }
        catch (_: Exception) { Tertiary }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Место
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(36.dp)
        ) {
            when (clan.rank) {
                1 -> Text("🥇", style = MaterialTheme.typography.titleMedium)
                2 -> Text("🥈", style = MaterialTheme.typography.titleMedium)
                3 -> Text("🥉", style = MaterialTheme.typography.titleMedium)
                else -> Text(
                    "${clan.rank}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Цветной блок клана
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Text(
                text = clan.tag.take(3),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clan.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${clan.membersCount} участников • ${clan.territoriesCount} территорий",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formatArea(clan.totalAreaM2),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}

@Composable
private fun LeaderboardError(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) { Text("Повторить") }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
