package com.sun.taiwan_stock_lab_android.feature.stocklist.data.repository

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.StockLocalDataSource
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.MarketSummaryRow
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.TwseRemoteDataSource
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.model.TwseRawSnapshot
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class OfflineFirstStockRepositoryTest {
    // See StockListViewModelTest for why asSnapshot() needs an explicit LoadStates signal.
    private val fullyLoadedStates =
        LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = true),
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        )

    @Test
    fun `observeStocksPaged maps entities to domain stocks`() =
        runTest {
            val localDataSource = mockk<StockLocalDataSource>()
            every { localDataSource.observeStocksPaged(SortDirection.DESCENDING) } returns
                flowOf(
                    PagingData.from(
                        listOf(sampleEntity("2330", "台積電")),
                        sourceLoadStates = fullyLoadedStates,
                    ),
                )
            val repository = OfflineFirstStockRepository(mockk(), localDataSource)
            val snapshot = repository.observeStocksPaged(SortDirection.DESCENDING).asSnapshot()
            assertEquals(1, snapshot.size)
            assertEquals("2330", snapshot.first().code)
        }

    @Test
    fun `getStock returns the mapped stock when the local data source finds it`() =
        runTest {
            val localDataSource = mockk<StockLocalDataSource>()
            coEvery { localDataSource.getStock("2330") } returns sampleEntity("2330", "台積電")
            val repository = OfflineFirstStockRepository(mockk(), localDataSource)
            val stock = repository.getStock("2330")
            assertEquals("2330", stock?.code)
        }

    @Test
    fun `getStock returns null when the local data source has no match`() =
        runTest {
            val localDataSource = mockk<StockLocalDataSource>()
            coEvery { localDataSource.getStock("9999") } returns null
            val repository = OfflineFirstStockRepository(mockk(), localDataSource)
            assertNull(repository.getStock("9999"))
        }

    @Test
    fun `observeMarketSummary maps the aggregate row to a domain summary`() =
        runTest {
            val localDataSource = mockk<StockLocalDataSource>()
            every { localDataSource.observeMarketSummary() } returns
                flowOf(MarketSummaryRow(advancingCount = 3, decliningCount = 1, unchangedCount = 2))
            val repository = OfflineFirstStockRepository(mockk(), localDataSource)
            val summary = repository.observeMarketSummary().first()
            assertEquals(3, summary.advancingCount)
            assertEquals(1, summary.decliningCount)
            assertEquals(2, summary.unchangedCount)
        }

    @Test
    fun `observeLastRefreshedAt reads timestamp from local data source`() =
        runTest {
            val localDataSource = mockk<StockLocalDataSource>()
            every { localDataSource.observeLastRefreshedAt() } returns flowOf(1_700_000_000_000L)
            val repository = OfflineFirstStockRepository(mockk(), localDataSource)
            assertEquals(1_700_000_000_000L, repository.observeLastRefreshedAt().first())
        }

    @Test
    fun `refreshStocks fetches remote data and writes to local data source`() =
        runTest {
            val remoteDataSource = mockk<TwseRemoteDataSource>()
            val localDataSource = mockk<StockLocalDataSource>(relaxUnitFun = true)
            coEvery { remoteDataSource.fetchSnapshot() } returns
                TwseRawSnapshot(
                    valuations = emptyList(),
                    dayAverages = emptyList(),
                    days = listOf(StockDayDto(code = "2330", name = "台積電")),
                )
            val repository = OfflineFirstStockRepository(remoteDataSource, localDataSource)
            val result = repository.refreshStocks()
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                localDataSource.replaceAll(match { it.size == 1 && it.first().code == "2330" }, any())
            }
        }

    @Test
    fun `refreshStocks writes the refresh timestamp along with stocks`() =
        runTest {
            val remoteDataSource = mockk<TwseRemoteDataSource>()
            val localDataSource = mockk<StockLocalDataSource>(relaxUnitFun = true)
            val fixedTime = 1_700_000_000_000L
            coEvery { remoteDataSource.fetchSnapshot() } returns
                TwseRawSnapshot(
                    valuations = emptyList(),
                    dayAverages = emptyList(),
                    days = listOf(StockDayDto(code = "2330", name = "台積電")),
                )
            val repository =
                OfflineFirstStockRepository(remoteDataSource, localDataSource, clock = { fixedTime })
            repository.refreshStocks()
            coVerify(exactly = 1) { localDataSource.replaceAll(any(), refreshedAt = fixedTime) }
        }

    @Test
    fun `refreshStocks whenNetworkFails keepsExistingCache`() =
        runTest {
            val remoteDataSource = mockk<TwseRemoteDataSource>()
            val localDataSource = mockk<StockLocalDataSource>(relaxUnitFun = true)
            coEvery { remoteDataSource.fetchSnapshot() } throws IOException("Network failure")
            val repository = OfflineFirstStockRepository(remoteDataSource, localDataSource)
            val result = repository.refreshStocks()
            assertTrue(result.isFailure)
            coVerify(exactly = 0) { localDataSource.replaceAll(any(), any()) }
        }

    @Test
    fun `refreshStocks when remote result is empty keeps existing cache`() =
        runTest {
            val remoteDataSource = mockk<TwseRemoteDataSource>()
            val localDataSource = mockk<StockLocalDataSource>(relaxUnitFun = true)
            coEvery { remoteDataSource.fetchSnapshot() } returns
                TwseRawSnapshot(
                    valuations = emptyList(),
                    dayAverages = emptyList(),
                    days = emptyList(),
                )
            val repository = OfflineFirstStockRepository(remoteDataSource, localDataSource)
            val result = repository.refreshStocks()
            assertTrue(result.isFailure)
            coVerify(exactly = 0) { localDataSource.replaceAll(any(), any()) }
        }

    @Test
    fun `refreshStocks propagates coroutine cancellation`() =
        runTest {
            val remoteDataSource = mockk<TwseRemoteDataSource>()
            val localDataSource = mockk<StockLocalDataSource>()
            coEvery { remoteDataSource.fetchSnapshot() } throws CancellationException()
            val repository = OfflineFirstStockRepository(remoteDataSource, localDataSource)
            assertThrows(CancellationException::class.java) {
                kotlinx.coroutines.runBlocking { repository.refreshStocks() }
            }
        }

    private fun sampleEntity(
        code: String,
        name: String,
    ) = StockEntity(
        code = code,
        name = name,
        openingPrice = null,
        highestPrice = null,
        lowestPrice = null,
        closingPrice = null,
        monthlyAveragePrice = null,
        change = null,
        tradeVolume = null,
        tradeValue = null,
        transactionCount = null,
        peRatio = null,
        dividendYield = null,
        pbRatio = null,
    )
}
