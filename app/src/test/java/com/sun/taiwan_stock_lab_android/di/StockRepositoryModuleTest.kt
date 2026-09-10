package com.sun.taiwan_stock_lab_android.di

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.StockLocalDataSource
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.TwseRemoteDataSource
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.api.TwseApiService
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.repository.OfflineFirstStockRepository
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class StockRepositoryModuleTest {
    @Test
    fun provideTwseRemoteDataSource_wrapsGivenApi() {
        val api = mockk<TwseApiService>()
        assertTrue(StockRepositoryModule.provideTwseRemoteDataSource(api) is TwseRemoteDataSource)
    }

    @Test
    fun provideStockLocalDataSource_wrapsGivenDao() {
        val dao = mockk<StockDao>()
        assertTrue(StockRepositoryModule.provideStockLocalDataSource(dao) is StockLocalDataSource)
    }

    @Test
    fun provideStockRepository_buildsOfflineFirstImplementation() {
        val remote = mockk<TwseRemoteDataSource>()
        val local = mockk<StockLocalDataSource>()
        assertTrue(StockRepositoryModule.provideStockRepository(remote, local) is OfflineFirstStockRepository)
    }
}
