package com.territorywars.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.territorywars.domain.model.Achievement
import com.territorywars.domain.model.AchievementCategory

data class AchievementDto(
    @SerializedName("id")          val id: String,
    @SerializedName("key")         val key: String,
    @SerializedName("name")        val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("category")    val category: String,
    @SerializedName("points")      val points: Int,
    @SerializedName("icon")        val icon: String,
    @SerializedName("unlocked_at") val unlockedAt: String? = null,
) {
    fun toDomain() = Achievement(
        id = id,
        key = key,
        name = name,
        description = description,
        category = AchievementCategory.from(category),
        points = points,
        icon = icon,
        unlockedAt = unlockedAt,
    )
}
