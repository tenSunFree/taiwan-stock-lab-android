package com.sun.taiwan_stock_lab_android.di

import com.sun.taiwan_stock_lab_android.fake.FakeStockRepository
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository.StockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [StockRepositoryModule::class],
)
object FakeStockRepositoryModule {
    @Provides
    @Singleton
    fun provideStockRepository(): StockRepository = FakeStockRepository()
}
