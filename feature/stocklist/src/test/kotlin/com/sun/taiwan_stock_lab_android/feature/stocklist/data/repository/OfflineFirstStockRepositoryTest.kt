package com.sun.taiwan_stock_lab_android.feature.stocklist.data.repository

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.TwseRemoteDataSource
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.model.TwseRawSnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class OfflineFirstStockRepositoryTest {

    @Test
    fun `observeStocks reads from Room`() = runTest {
        val stockDao = mockk<StockDao>()
        every { stockDao.observeAll() } returns flowOf(
            listOf(sampleEntity("2330", "台積電")),
        )
        val repository = OfflineFirstStockRepository(mockk(), stockDao)

        val stocks = repository.observeStocks().first()

        assertEquals(1, stocks.size)
        assertEquals("2330", stocks.first().code)
    }

    @Test
    fun `refreshStocks fetches remote data and writes to Room`() = runTest {
        val remoteDataSource = mockk<TwseRemoteDataSource>()
        val stockDao = mockk<StockDao>(relaxUnitFun = true)

        coEvery { remoteDataSource.fetchSnapshot() } returns TwseRawSnapshot(
            valuations = emptyList(),
            dayAverages = emptyList(),
            days = listOf(StockDayDto(code = "2330", name = "台積電")),
        )

        val repository = OfflineFirstStockRepository(remoteDataSource, stockDao)
        val result = repository.refreshStocks()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { stockDao.replaceAll(match { it.size == 1 && it.first().code == "2330" }) }
    }

    @Test
    fun `refreshStocks whenNetworkFails keepsExistingCache`() = runTest {
        val remoteDataSource = mockk<TwseRemoteDataSource>()
        val stockDao = mockk<StockDao>(relaxUnitFun = true)

        coEvery { remoteDataSource.fetchSnapshot() } throws IOException("Network failure")

        val repository = OfflineFirstStockRepository(remoteDataSource, stockDao)
        val result = repository.refreshStocks()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { stockDao.replaceAll(any()) }
    }

    @Test
    fun `refreshStocks when remote result is empty keeps existing cache`() = runTest {
        val remoteDataSource = mockk<TwseRemoteDataSource>()
        val stockDao = mockk<StockDao>(relaxUnitFun = true)

        coEvery { remoteDataSource.fetchSnapshot() } returns TwseRawSnapshot(
            valuations = emptyList(),
            dayAverages = emptyList(),
            days = emptyList(),
        )

        val repository = OfflineFirstStockRepository(remoteDataSource, stockDao)
        val result = repository.refreshStocks()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { stockDao.replaceAll(any()) }
    }

    @Test
    fun `refreshStocks propagates coroutine cancellation`() = runTest {
        val remoteDataSource = mockk<TwseRemoteDataSource>()
        val stockDao = mockk<StockDao>()

        coEvery { remoteDataSource.fetchSnapshot() } throws CancellationException()

        val repository = OfflineFirstStockRepository(remoteDataSource, stockDao)

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { repository.refreshStocks() }
        }
    }

    private fun sampleEntity(code: String, name: String) = StockEntity(
        code = code, name = name, openingPrice = null, highestPrice = null,
        lowestPrice = null, closingPrice = null, monthlyAveragePrice = null,
        change = null, tradeVolume = null, tradeValue = null, transactionCount = null,
        peRatio = null, dividendYield = null, pbRatio = null,
    )
}