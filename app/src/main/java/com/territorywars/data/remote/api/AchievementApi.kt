package com.territorywars.data.remote.api

import com.territorywars.data.remote.dto.AchievementDto
import retrofit2.Response
import retrofit2.http.GET

interface AchievementApi {
    @GET("achievements")
    suspend fun getAllAchievements(): Response<List<AchievementDto>>

    @GET("achievements/me")
    suspend fun getMyAchievements(): Response<List<AchievementDto>>
}
