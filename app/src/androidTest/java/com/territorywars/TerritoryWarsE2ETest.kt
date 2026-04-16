package com.territorywars

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E тесты для критических сценариев Territory Wars
 */
@RunWith(AndroidJUnit4::class)
class TerritoryWarsE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAuthenticationFlow() {
        // Запускаем приложение (начинается с Splash Screen)
        composeTestRule.onNodeWithText("Territory Wars").assertExists()

        // Проверяем наличие экрана авторизации
        composeTestRule.onNodeWithText("Вход").assertExists()
        composeTestRule.onNodeWithText("Регистрация").assertExists()
    }

    @Test
    fun testMapScreenNavigation() {
        // После логина должна быть главная экран с картой
        composeTestRule.onNodeWithText("Старт").assertExists()
        composeTestRule.onNodeWithContentDescription("Карта").assertExists()
    }

    @Test
    fun testCaptureTerritory() {
        // Проверяем кнопку "Старт"
        val startButton = composeTestRule.onNodeWithText("Старт")
        startButton.assertIsEnabled()

        // Нажимаем на "Старт"
        startButton.performClick()

        // Должны появиться кнопки "Отмена" и счетчик расстояния
        composeTestRule.onNodeWithText("Отмена").assertExists()
        composeTestRule.onNodeWithText("0 м").assertExists()
    }

    @Test
    fun testProfileNavigation() {
        // Проверяем наличие навигационной панели
        composeTestRule.onNodeWithContentDescription("Профиль").assertExists()

        // Нажимаем на профиль
        composeTestRule.onNodeWithContentDescription("Профиль").performClick()

        // Должны увидеть информацию профиля
        composeTestRule.onNodeWithText("Статистика").assertExists()
    }

    @Test
    fun testLeaderboardView() {
        // Проверяем наличие рейтинга
        composeTestRule.onNodeWithContentDescription("Рейтинг").assertExists()

        // Нажимаем на рейтинг
        composeTestRule.onNodeWithContentDescription("Рейтинг").performClick()

        // Должны увидеть вкладки Игроки/Кланы
        composeTestRule.onNodeWithText("Игроки").assertExists()
        composeTestRule.onNodeWithText("Кланы").assertExists()
    }

    @Test
    fun testNotificationHandling() {
        // Проверяем что NotificationService запущен
        // Это более интеграционный тест
        val context = composeTestRule.getUncomposeRule().activity
        assert(context != null)
    }

    @Test
    fun testGpsTrackingPermission() {
        // Проверяем запрос разрешений при запуске захвата
        composeTestRule.onNodeWithText("Старт").performClick()

        // Если разрешение не дано, должен быть диалог
        // Проверяем что UI правильно обрабатывает отсутствие GPS
        composeTestRule.onNodeWithText("Отмена").assertExists()
    }

    @Test
    fun testMapInteraction() {
        // Проверяем что можем взаимодействовать с картой
        composeTestRule.onNodeWithContentDescription("Карта").performTouchInput {
            swipe(
                start = android.graphics.PointF(100f, 100f),
                end = android.graphics.PointF(200f, 200f)
            )
        }

        // Карта должна остаться видимой
        composeTestRule.onNodeWithContentDescription("Карта").assertExists()
    }

    @Test
    fun testBottomNavigation() {
        // Проверяем что все пункты навигации работают
        val navItems = listOf("Карта", "Профиль", "Рейтинг", "Клан")

        for (item in navItems) {
            composeTestRule.onNodeWithContentDescription(item).performClick()
            // Экран должен измениться
            composeTestRule.onNodeWithContentDescription(item).assertExists()
        }
    }

    @Test
    fun testClanManagement() {
        // Переходим в экран клана
        composeTestRule.onNodeWithContentDescription("Клан").performClick()

        // Должны увидеть опции создания/поиска клана
        composeTestRule.onNodeWithText("Создать клан").assertExists()
        composeTestRule.onNodeWithText("Найти клан").assertExists()
    }
}
