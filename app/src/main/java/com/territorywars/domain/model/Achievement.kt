package com.territorywars.domain.model

data class Achievement(
    val id: String,
    val key: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val points: Int,
    val icon: String,
    val unlockedAt: String? = null,
) {
    val isUnlocked: Boolean get() = unlockedAt != null
}

enum class AchievementCategory(val displayName: String) {
    CAPTURE("Захват"),
    CONQUEST("Завоевание"),
    WALKING("Ходьба"),
    CLAN("Клан"),
    SPECIAL("Особые");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.name == value } ?: SPECIAL
    }
}
