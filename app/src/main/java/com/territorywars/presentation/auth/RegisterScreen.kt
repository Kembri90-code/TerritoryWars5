package com.territorywars.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.presentation.components.AppTextField
import com.territorywars.presentation.components.PrimaryButton
import com.territorywars.presentation.theme.Error
import com.territorywars.presentation.theme.Primary
import com.territorywars.presentation.theme.Success
import com.territorywars.presentation.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onRegisterSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Регистрация") },
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Никнейм
            AppTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChanged,
                label = "Никнейм",
                leadingIcon = Icons.Outlined.Person,
                error = state.usernameError,
                isValid = state.isUsernameValid,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Email
            AppTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = "Email",
                leadingIcon = Icons.Outlined.Email,
                error = state.emailError,
                isValid = state.isEmailValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Пароль с индикатором надёжности
            AppTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                label = "Пароль",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                error = state.passwordError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            // Индикатор надёжности пароля
            AnimatedVisibility(visible = state.password.isNotEmpty()) {
                PasswordStrengthIndicator(strength = state.passwordStrength)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Подтверждение пароля
            AppTextField(
                value = state.passwordConfirm,
                onValueChange = viewModel::onPasswordConfirmChanged,
                label = "Подтвердить пароль",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                error = state.passwordConfirmError,
                isValid = state.isPasswordConfirmValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Город (searchable dropdown)
            CityDropdown(
                query = state.cityQuery,
                onQueryChanged = viewModel::onCityQueryChanged,
                cities = state.cities,
                selectedCity = state.selectedCity,
                onCitySelected = viewModel::onCitySelected,
                error = state.cityError,
                isLoading = state.isCitiesLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ошибка сервера
            AnimatedVisibility(visible = state.serverError != null) {
                Text(
                    text = state.serverError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка «Зарегистрироваться»
            PrimaryButton(
                text = "Зарегистрироваться",
                onClick = viewModel::register,
                isLoading = state.isLoading,
                enabled = state.isFormValid && !state.isLoading
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp)) {
        val (color, label) = when (strength) {
            PasswordStrength.WEAK -> Error to "Слабый"
            PasswordStrength.MEDIUM -> Warning to "Средний"
            PasswordStrength.STRONG -> Success to "Сильный"
        }
        LinearProgressIndicator(
            progress = { when (strength) { PasswordStrength.WEAK -> 0.33f; PasswordStrength.MEDIUM -> 0.66f; PasswordStrength.STRONG -> 1f } },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 2.dp)
        )
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
    isLoading: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && cities.isNotEmpty(),
        onExpandedChange = { newExpanded ->
            expanded = newExpanded
            // При открытии — показываем все города (пустой запрос)
            if (newExpanded && query.isEmpty() && selectedCity == null) {
                onQueryChanged("")
            }
        }
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
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        ExposedDropdownMenu(
            expanded = expanded && cities.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            cities.forEach { city ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(city.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                city.region,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onCitySelected(city)
                        expanded = false
                    }
                )
            }
        }
    }
}
