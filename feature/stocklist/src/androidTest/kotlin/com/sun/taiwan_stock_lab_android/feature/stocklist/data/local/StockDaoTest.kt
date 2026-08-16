package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockDaoTest {

    private lateinit var database: StockDatabase
    private lateinit var dao: StockDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<StockDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.stockDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replaceAll_clearsPreviousDataAndInsertsNew() = runBlocking {
        dao.replaceAll(listOf(sampleEntity("2330")))
        dao.replaceAll(listOf(sampleEntity("0050")))

        val stocks = dao.observeAll().first()

        assertEquals(1, stocks.size)
        assertEquals("0050", stocks.single().code)
    }

    @Test
    fun observeAll_emitsStocksOrderedByCode() = runBlocking {
        dao.insertAll(listOf(sampleEntity("2330"), sampleEntity("0050"), sampleEntity("1101")))
        val stocks = dao.observeAll().first()
        assertEquals(listOf("0050", "1101", "2330"), stocks.map { it.code })
    }

    private fun sampleEntity(code: String) = StockEntity(
        code = code, name = "Test", openingPrice = null, highestPrice = null,
        lowestPrice = null, closingPrice = null, monthlyAveragePrice = null,
        change = null, tradeVolume = null, tradeValue = null, transactionCount = null,
        peRatio = null, dividendYield = null, pbRatio = null,
    )
}