package com.sun.taiwan_stock_lab_android.di

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.StockLocalDataSource
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.TwseRemoteDataSource
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.api.TwseApiService
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.repository.OfflineFirstStockRepository
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository.StockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StockRepositoryModule {
    @Provides
    @Singleton
    fun provideTwseRemoteDataSource(api: TwseApiService): TwseRemoteDataSource = TwseRemoteDataSource(api)

    @Provides
    @Singleton
    fun provideStockLocalDataSource(stockDao: StockDao): StockLocalDataSource = StockLocalDataSource(stockDao)

    @Provides
    @Singleton
    fun provideStockRepository(
        remoteDataSource: TwseRemoteDataSource,
        localDataSource: StockLocalDataSource,
    ): StockRepository = OfflineFirstStockRepository(remoteDataSource, localDataSource)
}
