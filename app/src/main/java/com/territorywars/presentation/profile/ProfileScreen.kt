package com.territorywars.presentation.profile

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.domain.model.User
import com.territorywars.presentation.map.AppBottomNav
import com.territorywars.presentation.map.PlayerMarker
import com.territorywars.presentation.theme.DmMono
import com.territorywars.presentation.theme.PresetColors
import com.territorywars.presentation.theme.PlusJakartaSans
import com.territorywars.presentation.theme.parseColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun formatArea(m2: Double): String = when {
    m2 < 1_000   -> "${m2.toInt()} м²"
    m2 < 1_000_000 -> "${"%.2f".format(m2 / 1_000_000)} км²"
    else          -> "${"%.0f".format(m2 / 1_000_000)} км²"
}
private fun formatDistance(m: Double): String = when {
    m < 1_000 -> "${m.toInt()} м"
    else      -> "${"%.1f".format(m / 1_000)} км"
}

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToClan: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLogout()
    }

    val bg      = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primary)
                }
                state.error != null && state.user == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(state.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = viewModel::loadProfile) { Text("Повторить") }
                    }
                }
                state.user != null -> ProfileContent(
                    user = state.user!!,
                    isSaving = state.isSaving,
                    selectedMarker = state.selectedMarker,
                    onColorChange = viewModel::changeColor,
                    onMarkerChange = viewModel::changeMarker,
                    onLogout = viewModel::logout,
                )
            }
        }

        AppBottomNav(
            active = "profile",
            onNavigate = { id ->
                when (id) {
                    "map"         -> onNavigateToMap()
                    "leaderboard" -> onNavigateToLeaderboard()
                    "clan"        -> onNavigateToClan()
                }
            },
        )
    }
}

@Composable
private fun ProfileContent(
    user: User,
    isSaving: Boolean,
    selectedMarker: PlayerMarker,
    onColorChange: (String) -> Unit,
    onMarkerChange: (PlayerMarker) -> Unit,
    onLogout: () -> Unit,
) {
    val userColor = remember(user.color) { parseColor(user.color) }
    val primary   = MaterialTheme.colorScheme.primary
    val bg        = MaterialTheme.colorScheme.background
    val surface   = MaterialTheme.colorScheme.surface
    val surfCont  = MaterialTheme.colorScheme.surfaceVariant
    val bgAlt     = MaterialTheme.colorScheme.surfaceContainerLow
    val outline   = MaterialTheme.colorScheme.outline
    val onBg      = MaterialTheme.colorScheme.onBackground
    val onSurfVar = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val errorCont  = MaterialTheme.colorScheme.errorContainer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.14f), surface)
                    )
                )
                .border(
                    width = 0.dp,
                    color = Color.Transparent,
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Профиль",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        color = onBg,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(errorCont)
                            .border(1.dp, errorColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { onLogout() },
                    ) {
                        Icon(Icons.Outlined.Logout, contentDescription = "Выйти", tint = errorColor, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Avatar
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(userColor.copy(alpha = 0.18f))
                            .border(3.dp, userColor, CircleShape),
                    ) {
                        Text(
                            text = user.username.take(2).uppercase(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            color = userColor,
                        )
                    }

                    Column {
                        Text(
                            text = user.username,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            color = onBg,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (user.clanTag != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(primary.copy(alpha = 0.15f))
                                        .border(1.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "[${user.clanTag}]",
                                        fontSize = 12.sp,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        color = primary,
                                    )
                                }
                            }
                            if (user.cityName != null) {
                                Text(
                                    text = "🏙 ${user.cityName}",
                                    fontSize = 12.sp,
                                    fontFamily = PlusJakartaSans,
                                    color = onSurfVar,
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = outline)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Stats ─────────────────────────────────────────────────────────
            SectionLabel("Статистика")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    label = "Площадь",
                    value = formatArea(user.totalAreaM2).substringBefore(" "),
                    unit = formatArea(user.totalAreaM2).substringAfterLast(" "),
                    iconTint = primary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Территории",
                    value = "${user.territoriesCount}",
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    label = "Захватов",
                    value = "${user.capturesCount}",
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Пройдено",
                    value = formatDistance(user.distanceWalkedM).substringBefore(" "),
                    unit = formatDistance(user.distanceWalkedM).substringAfterLast(" "),
                    iconTint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Color picker ──────────────────────────────────────────────────
            Column {
                SectionLabel("Цвет территорий")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Виден всем игрокам на карте",
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    color = onSurfVar,
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (isSaving) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = primary)
                    }
                } else {
                    ColorPickerGrid(selectedHex = user.color, onSelect = onColorChange)
                }
            }

            // ── Marker picker ─────────────────────────────────────────────────
            Column {
                SectionLabel("Маркер на карте")
                Spacer(modifier = Modifier.height(12.dp))
                MarkerPickerRow(
                    selectedMarker = selectedMarker,
                    userColor = userColor,
                    onSelect = onMarkerChange,
                    primary = primary,
                    outline = outline,
                    bgAlt = bgAlt,
                    onSurfVar = onSurfVar,
                )
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp,
    )
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    unit: String = "",
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val bg      = MaterialTheme.colorScheme.surfaceContainerLow
    val outline = MaterialTheme.colorScheme.outline
    val onBg    = MaterialTheme.colorScheme.onBackground
    val onSurfVar = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                color = onSurfVar,
                letterSpacing = 1.2.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = DmMono,
                    color = onBg,
                    letterSpacing = (-0.5).sp,
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = PlusJakartaSans,
                        color = onSurfVar,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPickerGrid(selectedHex: String, onSelect: (String) -> Unit) {
    val bg = MaterialTheme.colorScheme.background
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PresetColors.chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { hex ->
                    val color = remember(hex) { parseColor(hex) }
                    val selected = hex.equals(selectedHex, ignoreCase = true)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selected) Modifier.border(
                                    width = 2.dp,
                                    color = bg,
                                    shape = CircleShape,
                                ).border(
                                    width = 4.dp,
                                    color = color,
                                    shape = CircleShape,
                                ) else Modifier
                            )
                            .clickable { onSelect(hex) },
                    ) {
                        if (selected) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkerPickerRow(
    selectedMarker: PlayerMarker,
    userColor: Color,
    onSelect: (PlayerMarker) -> Unit,
    primary: Color,
    outline: Color,
    bgAlt: Color,
    onSurfVar: Color,
) {
    val entries = listOf(
        PlayerMarker.DOT     to "Точка",
        PlayerMarker.ARROW   to "Стрелка",
        PlayerMarker.STAR    to "Звезда",
        PlayerMarker.DIAMOND to "Ромб",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.forEach { (marker, label) ->
            val on = marker == selectedMarker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) primary.copy(alpha = 0.15f) else bgAlt)
                    .border(1.5.dp, if (on) primary else outline, RoundedCornerShape(14.dp))
                    .clickable { onSelect(marker) }
                    .padding(vertical = 13.dp),
            ) {
                Canvas(modifier = Modifier.size(22.dp)) {
                    drawMarkerShape(marker, if (on) userColor else onSurfVar)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontFamily = PlusJakartaSans,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) primary else onSurfVar,
                )
            }
        }
    }
}

private fun DrawScope.drawMarkerShape(marker: PlayerMarker, color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    when (marker) {
        PlayerMarker.DOT -> {
            drawCircle(color, radius = size.width / 2f)
            drawCircle(Color.White, radius = size.width / 6f)
        }
        PlayerMarker.ARROW -> {
            drawPath(Path().apply {
                moveTo(cx, 0f)
                lineTo(cx + size.width * 0.38f, cy + size.height * 0.32f)
                lineTo(cx, cy + size.height * 0.16f)
                lineTo(cx - size.width * 0.38f, cy + size.height * 0.32f)
                close()
            }, color)
        }
        PlayerMarker.STAR -> {
            drawPath(Path().also { path ->
                val outerR = size.width / 2f
                val innerR = size.width / 4.5f
                for (i in 0 until 10) {
                    val angle = (i * 36 - 90) * PI / 180.0
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = cx + r * cos(angle).toFloat()
                    val y = cy + r * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }, color)
        }
        PlayerMarker.DIAMOND -> {
            drawPath(Path().apply {
                moveTo(cx, 0f); lineTo(size.width, cy)
                lineTo(cx, size.height); lineTo(0f, cy); close()
            }, color)
        }
    }
}
