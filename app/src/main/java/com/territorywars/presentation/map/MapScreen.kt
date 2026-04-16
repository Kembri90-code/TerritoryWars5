package com.territorywars.presentation.map

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.territorywars.presentation.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToClan: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val locationPermission = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermission.allPermissionsGranted) {
            locationPermission.launchMultiplePermissionRequest()
        } else {
            viewModel.startLocationUpdates()
        }
    }

    LaunchedEffect(locationPermission.allPermissionsGranted) {
        if (locationPermission.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Яндекс MapKit View
        YandexMapView(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // Нет разрешений GPS
        if (!locationPermission.allPermissionsGranted) {
            GpsPermissionBanner(
                onRequest = { locationPermission.launchMultiplePermissionRequest() },
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
            )
        }

        // Кнопка «Моё местоположение» (справа, над кнопкой Старт)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingActionButton(
                onClick = viewModel::centerOnMyLocation,
                shape = CircleShape,
                containerColor = MapButtonBackground,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = "Моё местоположение", modifier = Modifier.size(22.dp))
            }
        }

        // Панель статуса захвата (сверху, при активном захвате)
        AnimatedVisibility(
            visible = state.isCapturing,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CaptureStatusPanel(
                distanceM = state.routeDistanceM,
                durationSec = state.captureDurationSec,
                distanceToStartM = state.distanceToStartM,
                modifier = Modifier.padding(top = 48.dp, start = 16.dp, end = 16.dp)
            )
        }

        // Нижняя панель управления + навигация
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Кнопки захвата
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!state.isCapturing) {
                    // Кнопка СТАРТ
                    Button(
                        onClick = viewModel::startCapture,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Начать захват", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Кнопка ОТМЕНА
                        OutlinedButton(
                            onClick = viewModel::cancelCapture,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Отмена", style = MaterialTheme.typography.labelLarge)
                        }

                        // Кнопка ЗАМКНУТЬ (только когда близко к старту)
                        AnimatedVisibility(visible = state.canFinishCapture) {
                            CloseContourButton(
                                onClick = viewModel::finishCapture,
                                modifier = Modifier.weight(1f).height(56.dp)
                            )
                        }
                    }
                }
            }

            // Навигационная панель
            NavigationBar(
                containerColor = MapOverlayBackground,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Outlined.Map, contentDescription = null) },
                    label = { Text("Карта", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("Профиль", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToLeaderboard,
                    icon = { Icon(Icons.Outlined.Leaderboard, contentDescription = null) },
                    label = { Text("Рейтинг", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToClan,
                    icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                    label = { Text("Клан", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Snackbar уведомления
        state.notification?.let { message ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp),
                dismissAction = {
                    IconButton(onClick = viewModel::dismissNotification) {
                        Icon(Icons.Outlined.Close, contentDescription = "Закрыть")
                    }
                }
            ) {
                Text(message)
            }
        }
    }
}

@Composable
private fun CloseContourButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Пульсирующая анимация
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
        modifier = modifier.scale(scale)
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Замкнуть", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CaptureStatusPanel(
    distanceM: Double,
    durationSec: Long,
    distanceToStartM: Double,
    modifier: Modifier = Modifier
) {
    val minutes = durationSec / 60
    val seconds = durationSec % 60

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MapOverlayBackground,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusChip(label = "Дистанция", value = if (distanceM >= 1000) "${"%.1f".format(distanceM / 1000)} км" else "${distanceM.toInt()} м")
            StatusChip(label = "Время", value = "%02d:%02d".format(minutes, seconds))
            StatusChip(label = "До старта", value = if (distanceToStartM > 1000) "∞" else "${distanceToStartM.toInt()} м")
        }
    }
}

@Composable
private fun StatusChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GpsPermissionBanner(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Нет доступа к GPS", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRequest) { Text("Разрешить") }
        }
    }
}
