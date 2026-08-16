package com.sun.taiwan_stock_lab_android.di

import com.sun.taiwan_stock_lab_android.BuildConfig
import com.sun.taiwan_stock_lab_android.core.network.NetworkClientFactory
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.api.TwseApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TwseNetworkModule {

    private const val TWSE_BASE_URL = "https://openapi.twse.com.tw/"

    @Provides
    @Singleton
    fun provideTwseRetrofit(): Retrofit =
        NetworkClientFactory.createRetrofit(
            baseUrl = TWSE_BASE_URL,
            enableLogging = BuildConfig.DEBUG,
        )

    @Provides
    @Singleton
    fun provideTwseApiService(retrofit: Retrofit): TwseApiService =
        retrofit.create(TwseApiService::class.java)
}