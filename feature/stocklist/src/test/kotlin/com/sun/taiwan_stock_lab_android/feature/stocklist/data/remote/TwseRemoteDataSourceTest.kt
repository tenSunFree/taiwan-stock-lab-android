package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.api.TwseApiService
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayAverageDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockValuationDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class TwseRemoteDataSourceTest {
    @Test
    fun `fetchSnapshot fetches all endpoints concurrently`() =
        runTest {
            val api = mockk<TwseApiService>()
            coEvery { api.getStockValuations() } coAnswers {
                delay(1_000.milliseconds)
                listOf(StockValuationDto(code = "2330"))
            }
            coEvery { api.getStockDayAverages() } coAnswers {
                delay(1_000.milliseconds)
                listOf(StockDayAverageDto(code = "2330"))
            }
            coEvery { api.getStockDays() } coAnswers {
                delay(1_000.milliseconds)
                listOf(StockDayDto(code = "2330", name = "台積電"))
            }
            val dataSource = TwseRemoteDataSource(api)
            val snapshot = dataSource.fetchSnapshot()
            assertEquals(1, snapshot.valuations.size)
            assertEquals(1, snapshot.dayAverages.size)
            assertEquals(1, snapshot.days.size)
            // If the three calls ran sequentially this would be 3000ms instead.
            assertEquals(1_000L, currentTime)
            coVerify(exactly = 1) { api.getStockValuations() }
            coVerify(exactly = 1) { api.getStockDayAverages() }
            coVerify(exactly = 1) { api.getStockDays() }
        }

    @Test
    fun `fetchSnapshot fails when any endpoint fails`() =
        runTest {
            val api = mockk<TwseApiService>()
            coEvery { api.getStockValuations() } returns emptyList()
            coEvery { api.getStockDayAverages() } throws IOException("Network failure")
            coEvery { api.getStockDays() } returns emptyList()
            val dataSource = TwseRemoteDataSource(api)
            assertThrows(IOException::class.java) {
                kotlinx.coroutines.runBlocking { dataSource.fetchSnapshot() }
            }
        }
}
