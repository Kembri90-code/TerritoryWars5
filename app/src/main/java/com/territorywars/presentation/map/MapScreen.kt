package com.territorywars.presentation.map

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
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

        // ── Capture status panel ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.isCapturing,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CaptureStatusPanel(
                distanceM = state.routeDistanceM,
                durationSec = state.captureDurationSec,
                distanceToStartM = state.distanceToStartM,
                canFinish = state.canFinishCapture,
                onCancel = viewModel::cancelCapture,
                onFinish = viewModel::finishCapture,
                primary = primary,
                tertiary = tertiary,
                glassBg = glassBg,
                outline = outline,
                errorColor = errorColor,
                errorCont = errorCont,
                onBg = onBg,
                onSurfVar = onSurfVar,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }

        // ── Territory legend (top-right) ──────────────────────────────────────
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

        // ── FABs (right side) ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = if (state.isCapturing) 170.dp else 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassFab(onClick = viewModel::centerOnMyLocation, glassBg = glassBg, outline = outline) {
                Icon(Icons.Outlined.MyLocation, contentDescription = "Мои координаты", tint = primary, modifier = Modifier.size(18.dp))
            }
        }

        // ── Start capture button ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = !state.isCapturing,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(Brush.linearGradient(listOf(primary, MaterialTheme.colorScheme.primaryContainer)))
                        .clickable { viewModel.startCapture() }
                        .padding(horizontal = 36.dp, vertical = 15.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Начать захват",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            letterSpacing = 0.3.sp,
                        )
                    }
                }
            }
        }

        // ── Bottom navigation bar ─────────────────────────────────────────────
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

// ── Capture status panel ──────────────────────────────────────────────────────

@Composable
private fun CaptureStatusPanel(
    distanceM: Double,
    durationSec: Long,
    distanceToStartM: Double,
    canFinish: Boolean,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
    primary: Color,
    tertiary: Color,
    glassBg: Color,
    outline: Color,
    errorColor: Color,
    errorCont: Color,
    onBg: Color,
    onSurfVar: Color,
    modifier: Modifier = Modifier,
) {
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    val distStr = if (distanceM >= 1000)
        "${"%.2f".format(distanceM / 1000)} км" else "${distanceM.toInt()} м"
    val toStartStr = if (distanceToStartM > 9999) "—" else "${distanceToStartM.toInt()} м"

    val progress = (distanceM / 300.0).coerceIn(0.0, 1.0).toFloat()

    // Pulse animation for finish button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = glassBg,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, primary.copy(alpha = 0.35f)),
    ) {
        Column {
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(outline),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Brush.horizontalGradient(listOf(primary, tertiary))),
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CaptureStatItem("Дистанция", distStr, onBg, onSurfVar)
                    VerticalDivider(modifier = Modifier.height(38.dp), color = outline)
                    CaptureStatItem("Время", "%02d:%02d".format(minutes, seconds), onBg, onSurfVar)
                    VerticalDivider(modifier = Modifier.height(38.dp), color = outline)
                    CaptureStatItem(
                        label = "До старта",
                        value = toStartStr,
                        valueColor = if (distanceToStartM <= 30.0) primary else onBg,
                        labelColor = onSurfVar,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cancel
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(errorCont)
                            .border(1.5.dp, errorColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { onCancel() },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = null, tint = errorColor, modifier = Modifier.size(13.dp))
                            Text("Отмена", color = errorColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        }
                    }

                    // Finish (only when canFinish)
                    AnimatedVisibility(visible = canFinish, modifier = Modifier.weight(2f)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(44.dp)
                                .scale(pulseScale)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(primary, tertiary)))
                                .clickable { onFinish() },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Text("Замкнуть", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureStatItem(label: String, value: String, valueColor: Color, labelColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 10.sp,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 19.sp,
            fontFamily = DmMono,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
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
        HorizontalDivider(color = outline)
        NavigationBar(
            containerColor = surface,
            tonalElevation = 0.dp,
            modifier = Modifier.navigationBarsPadding(),
        ) {
            items.forEach { (id, label, icon) ->
                val on = active == id
                NavigationBarItem(
                    selected = on,
                    onClick = { if (!on) onNavigate(id) },
                    icon = {
                        if (id == "clan" && clanBadge > 0) {
                            BadgedBox(badge = {
                                Badge { Text(clanBadge.toString(), fontSize = 9.sp) }
                            }) {
                                Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = primary,
                        selectedTextColor   = primary,
                        unselectedIconColor = onSurfVar,
                        unselectedTextColor = onSurfVar,
                        indicatorColor      = primary.copy(alpha = 0.15f),
                    ),
                )
            }
        }
    }
}
