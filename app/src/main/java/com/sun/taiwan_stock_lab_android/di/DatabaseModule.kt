package com.sun.taiwan_stock_lab_android.di

import android.content.Context
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.StockDatabase
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.StockDatabaseFactory
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStockDatabase(@ApplicationContext context: Context): StockDatabase =
        StockDatabaseFactory.create(context)

    @Provides
    @Singleton
    fun provideStockDao(database: StockDatabase): StockDao = database.stockDao()
}