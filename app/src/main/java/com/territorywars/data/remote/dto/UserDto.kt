package com.territorywars.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.territorywars.domain.model.User

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("color") val color: String,
    @SerializedName("city_id") val cityId: Int,
    @SerializedName("city_name") val cityName: String?,
    @SerializedName("clan_id") val clanId: String?,
    @SerializedName("clan_name") val clanName: String?,
    @SerializedName("clan_tag") val clanTag: String?,
    @SerializedName("total_area_m2") val totalAreaM2: Double,
    @SerializedName("territories_count") val territoriesCount: Int,
    @SerializedName("captures_count") val capturesCount: Int,
    @SerializedName("takeovers_count") val takeoversCount: Int,
    @SerializedName("distance_walked_m") val distanceWalkedM: Double,
    @SerializedName("created_at") val createdAt: String
) {
    fun toDomain() = User(
        id = id,
        email = email,
        username = username,
        avatarUrl = avatarUrl,
        color = color,
        cityId = cityId,
        cityName = cityName,
        clanId = clanId,
        clanName = clanName,
        clanTag = clanTag,
        totalAreaM2 = totalAreaM2,
        territoriesCount = territoriesCount,
        capturesCount = capturesCount,
        takeoversCount = takeoversCount,
        distanceWalkedM = distanceWalkedM,
        createdAt = createdAt
    )
}

data class UpdateProfileRequest(
    @SerializedName("username") val username: String?,
    @SerializedName("color") val color: String?,
    @SerializedName("city_id") val cityId: Int?
)
