package com.territorywars.data.remote.api

import com.territorywars.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<RefreshResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth/check-username")
    suspend fun checkUsername(@Query("username") username: String): Response<CheckAvailabilityResponse>

    @GET("auth/check-email")
    suspend fun checkEmail(@Query("email") email: String): Response<CheckAvailabilityResponse>
}
