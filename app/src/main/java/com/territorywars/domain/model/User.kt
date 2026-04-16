package com.territorywars.domain.model

data class User(
    val id: String,
    val email: String,
    val username: String,
    val avatarUrl: String?,
    val color: String,           // #RRGGBB
    val cityId: Int,
    val cityName: String?,
    val clanId: String?,
    val clanName: String?,
    val clanTag: String?,
    val totalAreaM2: Double,
    val territoriesCount: Int,
    val capturesCount: Int,
    val takeoversCount: Int,
    val distanceWalkedM: Double,
    val createdAt: String
)
