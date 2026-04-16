package com.territorywars.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.domain.model.User
import com.territorywars.presentation.theme.Primary
import com.territorywars.presentation.theme.TerritoryColors
import com.territorywars.presentation.theme.territoryColorByHex

// Форматирует площадь: м² → тыс. м² → га
private fun formatArea(m2: Double): String = when {
    m2 < 1_000 -> "${m2.toInt()} м²"
    m2 < 100_000 -> "${"%.1f".format(m2 / 10_000)} га"
    else -> "${"%.0f".format(m2 / 10_000)} га"
}

// Форматирует дистанцию: м → км
private fun formatDistance(m: Double): String = when {
    m < 1_000 -> "${m.toInt()} м"
    else -> "${"%.1f".format(m / 1_000)} км"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLogout()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::logout) {
                        Icon(Icons.Outlined.Logout, contentDescription = "Выйти", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
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
                state.error != null && state.user == null -> {
                    ErrorState(
                        message = state.error!!,
                        onRetry = viewModel::loadProfile,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.user != null -> {
                    ProfileContent(
                        user = state.user!!,
                        isSaving = state.isSaving,
                        onColorChange = viewModel::changeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    isSaving: Boolean,
    onColorChange: (String) -> Unit
) {
    val userColor = remember(user.color) {
        try { territoryColorByHex(user.color) } catch (_: Exception) { Primary }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Аватар (инициалы в цветном круге)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(userColor.copy(alpha = 0.2f))
                .border(3.dp, userColor, CircleShape)
        ) {
            Text(
                text = user.username.take(2).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = userColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Имя
        Text(
            text = user.username,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Клан + город
        if (user.clanTag != null || user.cityName != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.clanTag != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = userColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "[${user.clanTag}]",
                            style = MaterialTheme.typography.labelMedium,
                            color = userColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (user.cityName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
                if (user.cityName != null) {
                    Icon(
                        Icons.Outlined.LocationCity,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = user.cityName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))

        // Статистика
        Text(
            text = "Статистика",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Outlined.Map,
                label = "Площадь",
                value = formatArea(user.totalAreaM2),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Outlined.Flag,
                label = "Территории",
                value = "${user.territoriesCount}",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Outlined.EmojiEvents,
                label = "Захватов",
                value = "${user.capturesCount}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Outlined.DirectionsWalk,
                label = "Пройдено",
                value = formatDistance(user.distanceWalkedM),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))

        // Цвет территорий
        Text(
            text = "Цвет территорий",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        ColorPicker(
            selectedHex = user.color,
            isSaving = isSaving,
            onColorSelected = onColorChange
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorPicker(
    selectedHex: String,
    isSaving: Boolean,
    onColorSelected: (String) -> Unit
) {
    val colorHexList = listOf(
        "#E53935", "#FF6D00", "#FFD600", "#76FF03", "#00C853",
        "#1DE9B6", "#40C4FF", "#2979FF", "#D500F9", "#FF4081",
        "#FF1744", "#6D4C41"
    )

    if (isSaving) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        return
    }

    // Сетка 6x2
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colorHexList.chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { hex ->
                    val color = remember(hex) {
                        try { Color(android.graphics.Color.parseColor(hex).toLong() or 0xFF000000L) }
                        catch (_: Exception) { Primary }
                    }
                    val isSelected = hex.equals(selectedHex, ignoreCase = true)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onBackground,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(hex) }
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Повторить")
        }
    }
}
