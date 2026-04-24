package com.territorywars.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class PlayerLeaderboardDto(
    @SerializedName("rank") val rank: Int,
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("color") val color: String,
    @SerializedName("city_name") val cityName: String?,
    @SerializedName("clan_tag") val clanTag: String?,
    @SerializedName("total_area_m2") val totalAreaM2: Double,
    @SerializedName("territories_count") val territoriesCount: Int,
    @SerializedName("captures_count") val capturesCount: Int,
    @SerializedName("distance_walked_m") val distanceWalkedM: Double
)

data class ClanLeaderboardDto(
    @SerializedName("rank") val rank: Int,
    @SerializedName("clan_id") val clanId: String,
    @SerializedName("name") val name: String,
    @SerializedName("tag") val tag: String,
    @SerializedName("color") val color: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("total_area_m2") val totalAreaM2: Double,
    @SerializedName("members_count") val membersCount: Int,
    @SerializedName("territories_count") val territoriesCount: Int
)

interface LeaderboardApi {

    @GET("leaderboard/players")
    suspend fun getPlayersLeaderboard(
        @Query("sort") sort: String = "area", // area | captures | distance
        @Query("limit") limit: Int = 100
    ): Response<List<PlayerLeaderboardDto>>

    @GET("leaderboard/clans")
    suspend fun getClansLeaderboard(
        @Query("sort") sort: String = "area",
        @Query("limit") limit: Int = 50
    ): Response<List<ClanLeaderboardDto>>
}
