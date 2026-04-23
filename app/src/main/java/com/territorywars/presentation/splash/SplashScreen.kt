package com.territorywars.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.presentation.components.TerritoryLogo
import com.territorywars.presentation.theme.PlusJakartaSans
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background

    // Logo: scale 0.5 → 1.07 → 1.0 (spring bounce)
    val logoScale = remember { Animatable(0.5f) }
    val textAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val spinnerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow,
            )
        )
    }
    LaunchedEffect(Unit) {
        delay(350)
        textAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        delay(100)
        subtitleAlpha.animateTo(1f, animationSpec = tween(450, easing = FastOutSlowInEasing))
        delay(200)
        spinnerAlpha.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Authenticated   -> { delay(400); onNavigateToMap() }
            AuthState.Unauthenticated -> { delay(400); onNavigateToLogin() }
            AuthState.Loading         -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        // Background grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = primary.copy(alpha = 0.06f)
            val sw = 1f
            for (i in 1..6) {
                val y = size.height * i / 7f
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = sw)
            }
            for (i in 1..4) {
                val x = size.width * i / 5f
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = sw)
            }
        }

        // Radial glow behind logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.35f),
                    radius = size.width * 0.65f,
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width / 2f, size.height * 0.35f),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TerritoryLogo(
                modifier = Modifier
                    .size(88.dp)
                    .scale(logoScale.value),
                color = primary,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha.value),
            ) {
                Text(
                    text = "TERRITORY",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "WARS",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = primary,
                    letterSpacing = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Захвати свой город",
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(subtitleAlpha.value),
            )

            Spacer(modifier = Modifier.height(56.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(22.dp).alpha(spinnerAlpha.value),
                strokeWidth = 2.dp,
                color = primary,
            )
        }
    }
}
