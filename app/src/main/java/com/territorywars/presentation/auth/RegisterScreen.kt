package com.territorywars.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
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
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val primary    = MaterialTheme.colorScheme.primary
    val bg         = MaterialTheme.colorScheme.background
    val surface    = MaterialTheme.colorScheme.surface
    val outline    = MaterialTheme.colorScheme.outline
    val outlineVar = MaterialTheme.colorScheme.outlineVariant
    val onBg       = MaterialTheme.colorScheme.onBackground
    val onSurfVar  = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val warning    = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onRegisterSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
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
                    .border(1.dp, outline, RoundedCornerShape(10.dp))
                    .clickable { onNavigateBack() },
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад", tint = onBg, modifier = Modifier.size(18.dp))
            }
            TerritoryLogo(modifier = Modifier.size(26.dp), color = primary)
            Text(
                text = "Регистрация",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                color = onBg,
            )
        }
        HorizontalDivider(color = outlineVar)

        // ── Form ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Username
            AppTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChanged,
                label = "Никнейм",
                leadingIcon = Icons.Outlined.Person,
                error = state.usernameError,
                hint = if (state.username.isEmpty()) "3–20 символов" else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            // 2. Email
            AppTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = "Email",
                leadingIcon = Icons.Outlined.Email,
                error = state.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            // 3. Password + strength indicator
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Пароль",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    error = state.passwordError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )

                AnimatedVisibility(
                    visible = state.password.isNotEmpty() && state.passwordError == null,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150)),
                ) {
                    val strengthColor = when (state.passwordStrength) {
                        PasswordStrength.WEAK   -> errorColor
                        PasswordStrength.MEDIUM -> warning
                        PasswordStrength.STRONG -> primary
                    }
                    val strengthLabel = when (state.passwordStrength) {
                        PasswordStrength.WEAK   -> "Слабый"
                        PasswordStrength.MEDIUM -> "Средний"
                        PasswordStrength.STRONG -> "Надёжный"
                    }
                    val progress = when (state.passwordStrength) {
                        PasswordStrength.WEAK   -> 1
                        PasswordStrength.MEDIUM -> 2
                        PasswordStrength.STRONG -> 3
                    }
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(3) { idx ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (idx < progress) strengthColor else outline),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strengthLabel,
                            color = strengthColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }

            // 4. Confirm password
            AppTextField(
                value = state.passwordConfirm,
                onValueChange = viewModel::onPasswordConfirmChanged,
                label = "Подтвердить пароль",
                leadingIcon = Icons.Outlined.Shield,
                isPassword = true,
                error = state.passwordConfirmError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            // 5. City dropdown
            CityDropdown(
                query = state.cityQuery,
                onQueryChanged = viewModel::onCityQueryChanged,
                cities = state.cities,
                selectedCity = state.selectedCity,
                onCitySelected = viewModel::onCitySelected,
                error = state.cityError,
                isLoading = state.isCitiesLoading,
            )

            AnimatedVisibility(visible = state.serverError != null) {
                Text(
                    text = state.serverError ?: "",
                    color = errorColor,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            PrimaryButton(
                text = "Создать аккаунт",
                onClick = viewModel::register,
                isLoading = state.isLoading,
                enabled = state.isFormValid && !state.isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityDropdown(
    query: String,
    onQueryChanged: (String) -> Unit,
    cities: List<com.territorywars.domain.model.City>,
    selectedCity: com.territorywars.domain.model.City?,
    onCitySelected: (com.territorywars.domain.model.City) -> Unit,
    error: String?,
    isLoading: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && cities.isNotEmpty(),
        onExpandedChange = { expanded = it },
    ) {
        AppTextField(
            value = if (selectedCity != null) selectedCity.name else query,
            onValueChange = {
                onQueryChanged(it)
                expanded = true
            },
            label = "Город",
            leadingIcon = Icons.Outlined.LocationCity,
            error = error,
            isValid = selectedCity != null,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        ExposedDropdownMenu(
            expanded = expanded && cities.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            cities.forEach { city ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(city.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                city.region,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onCitySelected(city)
                        expanded = false
                    },
                )
            }
        }
    }
}
