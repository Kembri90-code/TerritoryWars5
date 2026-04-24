package com.territorywars.domain.model

data class PlayerLeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val color: String,
    val cityName: String?,
    val clanTag: String?,
    val totalAreaM2: Double,
    val territoriesCount: Int,
    val capturesCount: Int,
    val distanceWalkedM: Double
)

data class ClanLeaderboardEntry(
    val rank: Int,
    val clanId: String,
    val name: String,
    val tag: String,
    val color: String,
    val avatarUrl: String?,
    val totalAreaM2: Double,
    val membersCount: Int,
    val territoriesCount: Int
)
