package com.territorywars.presentation.map

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.territorywars.presentation.theme.DmMono
import com.territorywars.presentation.theme.PlusJakartaSans

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToClan: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val primary    = MaterialTheme.colorScheme.primary
    val tertiary   = MaterialTheme.colorScheme.tertiary
    val bg         = MaterialTheme.colorScheme.background
    val surface    = MaterialTheme.colorScheme.surface
    val outline    = MaterialTheme.colorScheme.outline
    val outlineVar = MaterialTheme.colorScheme.outlineVariant
    val errorColor = MaterialTheme.colorScheme.error
    val errorCont  = MaterialTheme.colorScheme.errorContainer
    val onBg       = MaterialTheme.colorScheme.onBackground
    val onSurfVar  = MaterialTheme.colorScheme.onSurfaceVariant

    val glassBg = surface.copy(alpha = 0.90f)

    val locationPermission = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    LaunchedEffect(Unit) {
        if (!locationPermission.allPermissionsGranted) locationPermission.launchMultiplePermissionRequest()
    }
    LaunchedEffect(locationPermission.allPermissionsGranted) {
        if (locationPermission.allPermissionsGranted) viewModel.startLocationUpdates()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Map ───────────────────────────────────────────────────────────────
        YandexMapView(viewModel = viewModel, modifier = Modifier.fillMaxSize())

        // ── GPS permission banner ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = !locationPermission.allPermissionsGranted,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(errorCont)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.LocationOff, contentDescription = null, tint = errorColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Нет доступа к GPS",
                    modifier = Modifier.weight(1f),
                    color = errorColor,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
                TextButton(onClick = { locationPermission.launchMultiplePermissionRequest() }) {
                    Text("Разрешить", color = errorColor, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Capture stats pill (дистанция + время, выезжает снизу) ──────────────
        AnimatedVisibility(
            visible = state.isCapturing,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val minutes = state.captureDurationSec / 60
            val seconds = state.captureDurationSec % 60
            val distStr = if (state.routeDistanceM >= 1000)
                "${"%.2f".format(state.routeDistanceM / 1000)} км"
            else "${state.routeDistanceM.toInt()} м"

            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(glassBg)
                    .border(1.dp, primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ДИСТАНЦИЯ", fontSize = 9.sp, fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold, color = onSurfVar, letterSpacing = 0.8.sp)
                    Text(distStr, fontSize = 20.sp, fontFamily = DmMono,
                        fontWeight = FontWeight.Bold, color = onBg, letterSpacing = (-0.5).sp)
                }
                VerticalDivider(modifier = Modifier.height(32.dp), color = outline)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ВРЕМЯ", fontSize = 9.sp, fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold, color = onSurfVar, letterSpacing = 0.8.sp)
                    Text("%02d:%02d".format(minutes, seconds), fontSize = 20.sp, fontFamily = DmMono,
                        fontWeight = FontWeight.Bold, color = onBg, letterSpacing = (-0.5).sp)
                }
            }
        }

        // ── Territory legend (top-right, только не в захвате) ────────────────
        if (!state.isCapturing) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LegendPill("Ваши", primary, glassBg, outline)
            }
        }

        // ── GPS FAB ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = if (state.isCapturing) 140.dp else 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassFab(onClick = viewModel::centerOnMyLocation, glassBg = glassBg, outline = outline) {
                Icon(Icons.Outlined.MyLocation, contentDescription = "Мои координаты",
                    tint = primary, modifier = Modifier.size(18.dp))
            }
        }

        // ── Старт / Завершить — всегда видны ─────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp, end = 16.dp,
                    bottom = if (state.isCapturing) 12.dp else 68.dp,
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Старт — активен только когда не захватываем
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (!state.isCapturing)
                            Brush.linearGradient(listOf(primary, MaterialTheme.colorScheme.primaryContainer))
                        else
                            Brush.linearGradient(listOf(surface, surface))
                    )
                    .border(
                        width = if (state.isCapturing) 1.dp else 0.dp,
                        color = if (state.isCapturing) outline else Color.Transparent,
                        shape = RoundedCornerShape(26.dp),
                    )
                    .clickable(enabled = !state.isCapturing && !state.isLoading) {
                        viewModel.startCapture()
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null,
                        tint = if (!state.isCapturing) Color.White else onSurfVar,
                        modifier = Modifier.size(16.dp))
                    Text("Старт",
                        color = if (!state.isCapturing) Color.White else onSurfVar,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
            }

            // Завершить — краснеет когда идёт захват
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (state.isCapturing) errorColor.copy(alpha = 0.12f) else surface)
                    .border(1.5.dp,
                        if (state.isCapturing) errorColor else outline,
                        RoundedCornerShape(26.dp))
                    .clickable(enabled = state.isCapturing && !state.isLoading) {
                        viewModel.finishCapture()
                    },
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp, color = errorColor)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                            tint = if (state.isCapturing) errorColor else onSurfVar,
                            modifier = Modifier.size(16.dp))
                        Text("Завершить",
                            color = if (state.isCapturing) errorColor else onSurfVar,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }

        // ── Bottom navigation bar (только не в захвате) ───────────────────────
        if (!state.isCapturing) {
            AppBottomNav(
                active = "map",
                clanBadge = state.clanRequestsBadge,
                onNavigate = { id ->
                    when (id) {
                        "profile"     -> onNavigateToProfile()
                        "leaderboard" -> onNavigateToLeaderboard()
                        "clan"        -> onNavigateToClan()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // ── Snackbar ──────────────────────────────────────────────────────────
        state.notification?.let { message ->
            AppSnackbar(
                message = message,
                onDismiss = viewModel::dismissNotification,
                primary = primary,
                errorColor = errorColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp),
            )
        }
    }
}

// ── Shared UI components ──────────────────────────────────────────────────────

@Composable
fun GlassFab(
    onClick: () -> Unit,
    glassBg: Color,
    outline: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(glassBg)
            .border(1.dp, outline, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        content = content,
    )
}

@Composable
fun LegendPill(label: String, color: Color, glassBg: Color, outline: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(glassBg)
            .border(1.dp, outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(label, fontSize = 10.sp, fontFamily = DmMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AppSnackbar(
    message: String,
    onDismiss: () -> Unit,
    primary: Color,
    errorColor: Color,
    modifier: Modifier = Modifier,
) {
    val isSuccess = message.startsWith("✓")
    val accentColor = if (isSuccess) primary else errorColor
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline = MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(surface)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.15f)),
        ) {
            Icon(
                if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = message.removePrefix("✓ "),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun AppBottomNav(
    active: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    clanBadge: Int = 0,
) {
    val primary    = MaterialTheme.colorScheme.primary
    val surface    = MaterialTheme.colorScheme.surface
    val outline    = MaterialTheme.colorScheme.outline
    val onSurfVar  = MaterialTheme.colorScheme.onSurfaceVariant

    val items = listOf(
        Triple("map", "Карта", Icons.Outlined.Map),
        Triple("profile", "Профиль", Icons.Outlined.Person),
        Triple("leaderboard", "Рейтинг", Icons.Outlined.Leaderboard),
        Triple("clan", "Клан", Icons.Outlined.Group),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = outline.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surface)
                .navigationBarsPadding()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (id, label, icon) ->
                val on = active == id
                val iconTint = if (on) primary else onSurfVar
                val textColor = if (on) primary else onSurfVar

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { if (!on) onNavigate(id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (id == "clan" && clanBadge > 0) {
                        BadgedBox(badge = {
                            Badge { Text(clanBadge.toString(), fontSize = 9.sp) }
                        }) {
                            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontFamily = PlusJakartaSans,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = textColor,
                    )
                }
            }
        }
    }
}
