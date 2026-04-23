package com.territorywars.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.presentation.components.AppTextField
import com.territorywars.presentation.components.PrimaryButton
import com.territorywars.presentation.components.TerritoryLogo
import com.territorywars.presentation.theme.PlusJakartaSans

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val primary   = MaterialTheme.colorScheme.primary
    val bg        = MaterialTheme.colorScheme.background
    val surfCont  = MaterialTheme.colorScheme.surfaceVariant
    val onBg      = MaterialTheme.colorScheme.onBackground
    val onSurfVar = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val errorCont  = MaterialTheme.colorScheme.errorContainer
    val outline    = MaterialTheme.colorScheme.outline

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Hero section ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            primary.copy(alpha = 0.14f),
                            bg,
                        )
                    )
                )
                .padding(top = 52.dp, bottom = 32.dp, start = 28.dp, end = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TerritoryLogo(modifier = Modifier.size(68.dp), color = primary)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Territory Wars",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        color = onBg,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Войдите в аккаунт",
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                        color = onSurfVar,
                    )
                }
            }
        }

        // ── Form ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = "Email",
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                error = state.emailError,
            )

            AppTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                label = "Пароль",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus(); viewModel.login() }
                ),
                error = state.passwordError,
            )

            // Server error banner
            AnimatedVisibility(
                visible = state.serverError != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(errorCont)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = state.serverError ?: "",
                        color = errorColor,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            PrimaryButton(
                text = "Войти",
                onClick = viewModel::login,
                isLoading = state.isLoading,
                enabled = state.isFormValid && !state.isLoading,
            )

            // Divider "или"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = outline)
                Text(
                    text = "  или  ",
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                    color = onSurfVar,
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = outline)
            }

            // Register button
            OutlinedButton(
                onClick = onNavigateToRegister,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = onBg),
            ) {
                Text(
                    text = "Создать аккаунт",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
