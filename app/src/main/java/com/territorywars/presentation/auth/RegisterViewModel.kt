package com.territorywars.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.territorywars.data.local.TokenDataStore
import com.territorywars.data.remote.api.AuthApi
import com.territorywars.data.remote.api.CityApi
import com.territorywars.data.remote.dto.RegisterRequest
import com.territorywars.domain.model.City
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// Предустановленный список городов — используется когда сервер недоступен
private val FALLBACK_CITIES = listOf(
    City(1,  "Москва",          "Московская область",        13000000, 55.7558, 37.6173),
    City(2,  "Санкт-Петербург", "Ленинградская область",      5600000, 59.9311, 30.3609),
    City(3,  "Новосибирск",     "Новосибирская область",      1600000, 54.9884, 82.8977),
    City(4,  "Екатеринбург",    "Свердловская область",       1500000, 56.8389, 60.6057),
    City(5,  "Казань",          "Республика Татарстан",       1300000, 55.7964, 49.1082),
    City(6,  "Нижний Новгород", "Нижегородская область",      1200000, 56.2965, 43.9361),
    City(7,  "Челябинск",       "Челябинская область",        1100000, 55.1644, 61.4368),
    City(8,  "Самара",          "Самарская область",          1100000, 53.2007, 50.1500),
    City(9,  "Омск",            "Омская область",             1100000, 54.9885, 73.3242),
    City(10, "Ростов-на-Дону",  "Ростовская область",         1100000, 47.2357, 39.7015),
    City(11, "Уфа",             "Республика Башкортостан",    1100000, 54.7388, 55.9721),
    City(12, "Красноярск",      "Красноярский край",          1100000, 56.0153, 92.8932),
    City(13, "Пермь",           "Пермский край",              1000000, 58.0105, 56.2502),
    City(14, "Воронеж",         "Воронежская область",        1000000, 51.6720, 39.1843),
    City(15, "Волгоград",       "Волгоградская область",       900000, 48.7080, 44.5133),
    City(16, "Краснодар",       "Краснодарский край",          900000, 45.0355, 38.9753),
    City(17, "Саратов",         "Саратовская область",         800000, 51.5336, 46.0340),
    City(18, "Тюмень",          "Тюменская область",           800000, 57.1522, 65.5272),
    City(19, "Тольятти",        "Самарская область",           700000, 53.5303, 49.3461),
    City(20, "Ижевск",          "Республика Удмуртия",         650000, 56.8527, 53.2114),
    City(21, "Барнаул",         "Алтайский край",              630000, 53.3606, 83.7636),
    City(22, "Иркутск",         "Иркутская область",           620000, 52.2978, 104.2964),
    City(23, "Хабаровск",       "Хабаровский край",            600000, 48.4827, 135.0840),
    City(24, "Ульяновск",       "Ульяновская область",         600000, 54.3282, 48.3866),
    City(25, "Ярославль",       "Ярославская область",         590000, 57.6261, 39.8845),
    City(26, "Владивосток",     "Приморский край",             600000, 43.1332, 131.9113),
    City(27, "Махачкала",       "Республика Дагестан",         600000, 42.9849, 47.5047),
    City(28, "Томск",           "Томская область",             570000, 56.4977, 84.9744),
    City(29, "Оренбург",        "Оренбургская область",        570000, 51.7727, 55.1001),
    City(30, "Кемерово",        "Кемеровская область",         550000, 55.3904, 86.0478),
    City(31, "Новокузнецк",     "Кемеровская область",         540000, 53.7557, 87.1099),
    City(32, "Рязань",          "Рязанская область",           530000, 54.6295, 39.7425),
    City(33, "Астрахань",       "Астраханская область",        520000, 46.3497, 48.0408),
    City(34, "Набережные Челны","Республика Татарстан",        520000, 55.7428, 52.4058),
    City(35, "Пенза",           "Пензенская область",          510000, 53.1950, 45.0183),
    City(36, "Киров",           "Кировская область",           500000, 58.6035, 49.6680),
    City(37, "Тула",            "Тульская область",            470000, 54.1961, 37.6182),
    City(38, "Липецк",          "Липецкая область",            470000, 52.6088, 39.5992),
    City(39, "Чебоксары",       "Республика Чувашия",          450000, 56.1439, 47.2489),
    City(40, "Калининград",     "Калининградская область",     450000, 54.7104, 20.4522),
)

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

data class RegisterState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val cityQuery: String = "",
    val selectedCity: City? = null,
    val cities: List<City> = emptyList(),
    val isCitiesLoading: Boolean = false,
    val usernameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordConfirmError: String? = null,
    val cityError: String? = null,
    val serverError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isUsernameValid: Boolean = false,
    val isEmailValid: Boolean = false,
    val isPasswordConfirmValid: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.WEAK
) {
    val isFormValid: Boolean
        get() = isUsernameValid && isEmailValid &&
                passwordError == null && password.length >= 8 &&
                isPasswordConfirmValid && selectedCity != null
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val cityApi: CityApi,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    private var usernameCheckJob: Job? = null
    private var emailCheckJob: Job? = null
    private var citySearchJob: Job? = null

    init {
        loadCities()
    }

    // Кэш всех городов (с сервера или из fallback)
    private var allCities: List<City> = emptyList()

    private fun loadCities() {
        viewModelScope.launch {
            _state.update { it.copy(isCitiesLoading = true) }
            try {
                val response = cityApi.getCities()
                if (response.isSuccessful) {
                    allCities = response.body()!!.map { dto -> dto.toDomain() }
                } else {
                    allCities = FALLBACK_CITIES
                }
            } catch (_: Exception) {
                allCities = FALLBACK_CITIES
            }
            _state.update { it.copy(isCitiesLoading = false) }
        }
    }

    fun onUsernameChanged(value: String) {
        _state.update {
            it.copy(
                username = value,
                usernameError = validateUsernameLocally(value),
                isUsernameValid = false,
                serverError = null
            )
        }
        if (validateUsernameLocally(value) == null) {
            checkUsernameDebounced(value)
        }
    }

    private fun validateUsernameLocally(value: String): String? = when {
        value.length < 3 -> "Минимум 3 символа"
        value.length > 20 -> "Максимум 20 символов"
        !value.matches(Regex("^[a-zA-Zа-яА-ЯёЁ0-9_]+$")) -> "Допустимы только буквы, цифры и _"
        else -> null
    }

    private fun checkUsernameDebounced(username: String) {
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            delay(500)
            try {
                val response = authApi.checkUsername(username)
                if (response.isSuccessful) {
                    val available = response.body()!!.available
                    _state.update {
                        it.copy(
                            usernameError = if (!available) "Никнейм уже занят" else null,
                            isUsernameValid = available
                        )
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun onEmailChanged(value: String) {
        _state.update {
            it.copy(
                email = value,
                emailError = validateEmailLocally(value),
                isEmailValid = false,
                serverError = null
            )
        }
        if (validateEmailLocally(value) == null) {
            checkEmailDebounced(value)
        }
    }

    private fun validateEmailLocally(value: String): String? =
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) null
        else "Некорректный email"

    private fun checkEmailDebounced(email: String) {
        emailCheckJob?.cancel()
        emailCheckJob = viewModelScope.launch {
            delay(500)
            try {
                val response = authApi.checkEmail(email)
                if (response.isSuccessful) {
                    val available = response.body()!!.available
                    _state.update {
                        it.copy(
                            emailError = if (!available) "Этот email уже зарегистрирован" else null,
                            isEmailValid = available
                        )
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun onPasswordChanged(value: String) {
        _state.update {
            it.copy(
                password = value,
                passwordError = validatePassword(value),
                passwordStrength = calcStrength(value),
                passwordConfirmError = if (it.passwordConfirm.isNotEmpty() && it.passwordConfirm != value)
                    "Пароли не совпадают" else null,
                isPasswordConfirmValid = it.passwordConfirm == value && it.passwordConfirm.isNotEmpty()
            )
        }
    }

    private fun validatePassword(value: String): String? = when {
        value.length < 8 -> "Минимум 8 символов"
        !value.any { it.isDigit() } -> "Нужна хотя бы 1 цифра"
        !value.any { it.isUpperCase() } -> "Нужна хотя бы 1 заглавная буква"
        else -> null
    }

    private fun calcStrength(value: String): PasswordStrength {
        var score = 0
        if (value.length >= 8) score++
        if (value.any { it.isDigit() }) score++
        if (value.any { it.isUpperCase() }) score++
        if (value.any { !it.isLetterOrDigit() }) score++
        if (value.length >= 12) score++
        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    fun onPasswordConfirmChanged(value: String) {
        val match = value == _state.value.password
        _state.update {
            it.copy(
                passwordConfirm = value,
                passwordConfirmError = if (value.isNotEmpty() && !match) "Пароли не совпадают" else null,
                isPasswordConfirmValid = match && value.isNotEmpty()
            )
        }
    }

    fun onCityQueryChanged(query: String) {
        _state.update { it.copy(cityQuery = query, selectedCity = null, cityError = null) }
        citySearchJob?.cancel()
        citySearchJob = viewModelScope.launch {
            delay(200)
            val filtered = if (query.isBlank()) {
                allCities.take(20)
            } else {
                allCities.filter { it.name.contains(query, ignoreCase = true) }.take(20)
            }
            // Если кэш уже есть — отображаем сразу, иначе пробуем сеть
            if (filtered.isNotEmpty()) {
                _state.update { it.copy(cities = filtered) }
            } else {
                try {
                    val response = cityApi.getCities(search = query)
                    if (response.isSuccessful) {
                        val serverCities = response.body()!!.map { dto -> dto.toDomain() }
                        if (serverCities.isNotEmpty()) allCities = serverCities
                        _state.update { it.copy(cities = serverCities) }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun onCitySelected(city: City) {
        _state.update { it.copy(selectedCity = city, cityQuery = city.name, cityError = null) }
    }

    fun register() {
        val s = _state.value
        if (!s.isFormValid) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, serverError = null) }
            try {
                val response = authApi.register(
                    RegisterRequest(
                        username = s.username.trim(),
                        email = s.email.trim(),
                        password = s.password,
                        cityId = s.selectedCity!!.id
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenDataStore.saveTokens(body.accessToken, body.refreshToken)
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _state.update {
                        it.copy(isLoading = false, serverError = "Ошибка регистрации. Попробуйте позже")
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, serverError = "Нет подключения к интернету")
                }
            }
        }
    }
}
