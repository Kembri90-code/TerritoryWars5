package com.territorywars.domain.model

data class Clan(
    val id: String,
    val name: String,
    val tag: String,
    val leaderId: String,
    val color: String,
    val description: String?,
    val totalAreaM2: Double,
    val membersCount: Int,
    val maxMembers: Int,
    val createdAt: String
)

data class ClanMember(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val color: String,
    val role: ClanRole,
    val totalAreaM2: Double,
    val joinedAt: String
)

enum class ClanRole { LEADER, OFFICER, MEMBER }
