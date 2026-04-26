package com.territorywars.di

import com.territorywars.BuildConfig
import com.territorywars.data.remote.AuthInterceptor
import com.territorywars.data.remote.TokenAuthenticator
import com.territorywars.data.remote.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, tokenAuthenticator: TokenAuthenticator): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun provideTerritoryApi(retrofit: Retrofit): TerritoryApi =
        retrofit.create(TerritoryApi::class.java)

    @Provides @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)

    @Provides @Singleton
    fun provideClanApi(retrofit: Retrofit): ClanApi =
        retrofit.create(ClanApi::class.java)

    @Provides @Singleton
    fun provideLeaderboardApi(retrofit: Retrofit): LeaderboardApi =
        retrofit.create(LeaderboardApi::class.java)

    @Provides @Singleton
    fun provideCityApi(retrofit: Retrofit): CityApi =
        retrofit.create(CityApi::class.java)

    @Provides @Singleton
    fun provideAchievementApi(retrofit: Retrofit): AchievementApi =
        retrofit.create(AchievementApi::class.java)
}
