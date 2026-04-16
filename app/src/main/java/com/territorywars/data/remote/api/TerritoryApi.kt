package com.territorywars.data.remote.api

import com.territorywars.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface TerritoryApi {

    @GET("territories")
    suspend fun getTerritoriesInBbox(
        @Query("bbox") bbox: String // "lat1,lng1,lat2,lng2"
    ): Response<List<TerritoryDto>>

    @GET("territories/{id}")
    suspend fun getTerritoryById(@Path("id") id: String): Response<TerritoryDto>

    @POST("territories/capture")
    suspend fun captureTerritory(@Body request: CaptureRequest): Response<CaptureResponse>

    @GET("territories/my")
    suspend fun getMyTerritories(): Response<List<TerritoryDto>>

    @GET("users/{id}/territories")
    suspend fun getUserTerritories(@Path("id") userId: String): Response<List<TerritoryDto>>
}
