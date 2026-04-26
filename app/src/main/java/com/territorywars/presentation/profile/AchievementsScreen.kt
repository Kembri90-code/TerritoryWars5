package com.territorywars.presentation.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.domain.model.Achievement
import com.territorywars.domain.model.AchievementCategory
import com.territorywars.presentation.theme.DmMono

@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.load() }) { Text("Повторить") }
            }
        }
        return
    }

    val unlocked = state.unlocked
    val available = state.all.filter { !it.isUnlocked }
    val totalPoints = state.totalPoints
    val maxPoints = state.all.sumOf { it.points }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header summary
        item {
            AchievementsSummary(
                unlocked = unlocked.size,
                total = state.all.size,
                points = totalPoints,
                maxPoints = maxPoints,
            )
            Spacer(Modifier.height(16.dp))
        }

        // Unlocked section
        if (unlocked.isNotEmpty()) {
            item {
                SectionHeader("Выполненные", unlocked.size, color = MaterialTheme.colorScheme.primary)
            }
            items(unlocked, key = { it.id }) { ach ->
                AchievementCard(achievement = ach, isUnlocked = true)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // Available section
        if (available.isNotEmpty()) {
            item {
                SectionHeader("Доступные", available.size, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Group by category
            val byCategory = available.groupBy { it.category }
            AchievementCategory.entries.forEach { cat ->
                val items = byCategory[cat] ?: return@forEach
                item {
                    Text(
                        cat.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                    )
                }
                items(items, key = { it.id }) { ach ->
                    AchievementCard(achievement = ach, isUnlocked = false)
                }
            }
        }
    }
}

@Composable
private fun AchievementsSummary(unlocked: Int, total: Int, points: Int, maxPoints: Int) {
    val progress = if (maxPoints > 0) points.toFloat() / maxPoints else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Достижения", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "$unlocked / $total выполнено",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$points",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        fontFamily = DmMono,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("очков", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = color)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.12f),
        ) {
            Text(
                "$count",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement, isUnlocked: Boolean) {
    val alpha = if (isUnlocked) 1f else 0.45f
    val borderColor = if (isUnlocked)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isUnlocked)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(achievement.icon, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isUnlocked)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Text(
                    achievement.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Points badge
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUnlocked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                ) {
                    Text(
                        "+${achievement.points}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmMono,
                        color = if (isUnlocked)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isUnlocked && achievement.unlockedAt != null) {
                    Text(
                        formatDate(achievement.unlockedAt),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private fun formatDate(iso: String): String = try {
    iso.take(10).replace("-", ".")
} catch (_: Exception) { "" }
