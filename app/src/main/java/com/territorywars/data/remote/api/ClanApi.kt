package com.territorywars.data.remote.api

import com.territorywars.data.remote.dto.TerritoryDto
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

data class ClanJoinRequestDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("color") val color: String,
    @SerializedName("total_area_m2") val totalAreaM2: Double,
    @SerializedName("created_at") val createdAt: String
)

data class CreateClanRequest(
    @SerializedName("name") val name: String,
    @SerializedName("tag") val tag: String,
    @SerializedName("color") val color: String,
    @SerializedName("description") val description: String?
)

data class ClanDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("tag") val tag: String,
    @SerializedName("leader_id") val leaderId: String,
    @SerializedName("color") val color: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("total_area_m2") val totalAreaM2: Double,
    @SerializedName("members_count") val membersCount: Int,
    @SerializedName("max_members") val maxMembers: Int,
    @SerializedName("created_at") val createdAt: String
)

data class ClanActivityDto(
    @SerializedName("territory_id") val territoryId: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("owner_username") val ownerUsername: String,
    @SerializedName("owner_color") val ownerColor: String,
    @SerializedName("area_m2") val areaM2: Double,
    @SerializedName("captured_at") val capturedAt: String
)

data class ClanMemberDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("color") val color: String,
    @SerializedName("role") val role: String,
    @SerializedName("total_area_m2") val totalAreaM2: Double,
    @SerializedName("joined_at") val joinedAt: String
)

interface ClanApi {

    @POST("clans")
    suspend fun createClan(@Body request: CreateClanRequest): Response<ClanDto>

    @GET("clans/{id}")
    suspend fun getClanById(@Path("id") id: String): Response<ClanDto>

    @GET("clans/{id}/members")
    suspend fun getClanMembers(@Path("id") id: String): Response<List<ClanMemberDto>>

    @POST("clans/{id}/join")
    suspend fun joinClan(@Path("id") id: String): Response<Unit>

    @POST("clans/{id}/leave")
    suspend fun leaveClan(@Path("id") id: String): Response<Unit>

    @DELETE("clans/{id}/members/{userId}")
    suspend fun kickMember(
        @Path("id") clanId: String,
        @Path("userId") userId: String
    ): Response<Unit>

    @DELETE("clans/{id}")
    suspend fun deleteClan(@Path("id") id: String): Response<Unit>

    @POST("clans/{id}/request")
    suspend fun requestJoinClan(@Path("id") id: String): Response<Unit>

    @GET("clans/{id}/requests")
    suspend fun getClanRequests(@Path("id") id: String): Response<List<ClanJoinRequestDto>>

    @POST("clans/{id}/requests/{userId}/accept")
    suspend fun acceptJoinRequest(
        @Path("id") clanId: String,
        @Path("userId") userId: String
    ): Response<Unit>

    @DELETE("clans/{id}/requests/{userId}")
    suspend fun declineJoinRequest(
        @Path("id") clanId: String,
        @Path("userId") userId: String
    ): Response<Unit>

    @PUT("clans/{id}")
    suspend fun updateClan(
        @Path("id") id: String,
        @Body request: CreateClanRequest
    ): Response<ClanDto>

    @GET("clans/{id}/territories")
    suspend fun getClanTerritories(@Path("id") id: String): Response<List<TerritoryDto>>

    @GET("clans/{id}/activity")
    suspend fun getClanActivity(
        @Path("id") id: String,
        @Query("limit") limit: Int = 30
    ): Response<List<ClanActivityDto>>

    @Multipart
    @POST("clans/{id}/avatar")
    suspend fun uploadClanAvatar(
        @Path("id") id: String,
        @Part avatar: MultipartBody.Part
    ): Response<ClanDto>
}
