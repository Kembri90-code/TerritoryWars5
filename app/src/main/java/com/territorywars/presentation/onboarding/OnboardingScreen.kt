package com.territorywars.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.territorywars.presentation.theme.PlusJakartaSans
import kotlinx.coroutines.launch

private data class PageData(
    val emoji: String,
    val title: String,
    val description: String,
    val accent: Color,
)

private val PAGES = listOf(
    PageData(
        emoji = "🗺️",
        title = "Добро пожаловать!",
        description = "Territory Wars — захватывай территории, обходя их пешком. Соревнуйся с другими игроками за площадь на карте своего города.",
        accent = Color(0xFF2979FF),
    ),
    PageData(
        emoji = "📍",
        title = "Захват территорий",
        description = "Нажми «Старт» и начинай идти по периметру нужного участка. Замкни маршрут и нажми «Завершить» — полигон станет твоим!",
        accent = Color(0xFF00C853),
    ),
    PageData(
        emoji = "👥",
        title = "Кланы",
        description = "Создай собственный клан или вступи в существующий. Территории участников объединяются — чем больше команда, тем больше влияние.",
        accent = Color(0xFFD500F9),
    ),
    PageData(
        emoji = "🏆",
        title = "Начни играть!",
        description = "Всё готово! Выйди на улицу и захвати свою первую территорию. Следи за рейтингом и борись за лидерство.",
        accent = Color(0xFFFF6D00),
    ),
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val currentPage = PAGES[pagerState.currentPage]
    val isLast = pagerState.currentPage == PAGES.lastIndex
    val bg = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize().background(bg)) {

        // Слайды
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            PageContent(page = PAGES[index], bg = bg)
        }

        // Кнопка «Пропустить»
        TextButton(
            onClick = onComplete,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 8.dp),
        ) {
            Text("Пропустить", fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }

        // Нижняя панель
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Индикатор страниц
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                PAGES.forEachIndexed { index, page ->
                    val selected = pagerState.currentPage == index
                    val dotColor by animateColorAsState(
                        if (selected) page.accent else MaterialTheme.colorScheme.outlineVariant,
                        label = "dot$index",
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(dotColor)
                            .size(if (selected) 10.dp else 7.dp),
                    )
                }
            }

            // Кнопка Далее / Начать
            Button(
                onClick = {
                    if (isLast) onComplete()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = currentPage.accent),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
                Text(
                    text = if (isLast) "Начать!" else "Далее",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PageContent(page: PageData, bg: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(page.accent.copy(alpha = 0.09f), bg)))
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))

        Text(page.emoji, fontSize = 80.sp, modifier = Modifier.padding(bottom = 28.dp))

        Text(
            text = page.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = page.description,
            fontSize = 15.sp,
            fontFamily = PlusJakartaSans,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp,
        )

        Spacer(Modifier.weight(1.5f))
        Spacer(Modifier.height(140.dp))  // место под нижнюю панель
    }
}
