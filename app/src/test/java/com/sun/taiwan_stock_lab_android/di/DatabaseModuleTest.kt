package com.sun.taiwan_stock_lab_android.di

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.StockDatabase
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Test

class DatabaseModuleTest {
    @Test
    fun provideStockDao_delegatesToDatabase() {
        val dao = mockk<StockDao>()
        val database = mockk<StockDatabase> { every { stockDao() } returns dao }
        assertSame(dao, DatabaseModule.provideStockDao(database))
    }
}
