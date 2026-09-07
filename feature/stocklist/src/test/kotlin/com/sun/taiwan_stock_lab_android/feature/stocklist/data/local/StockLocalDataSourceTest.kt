package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.asSnapshot
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.MarketSummaryRow
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.RefreshMetadataEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * StockLocalDataSource is mocked away in OfflineFirstStockRepositoryTest, so its own logic
 * (direction -> DAO source selection, Pager wiring, metadata mapping) has no JVM coverage yet.
 * This exercises the class directly against a mocked StockDao.
 */
class StockLocalDataSourceTest {
    private val stockDao = mockk<StockDao>()
    private val dataSource = StockLocalDataSource(stockDao)

    @Test
    fun `observeStocksPaged with ascending direction uses ascending DAO query`() =
        runTest {
            every {
                stockDao.observeAllAscendingPaged()
            } returns fakePagingSource(sampleEntity("2317"), sampleEntity("2330"))
            val snapshot = dataSource.observeStocksPaged(SortDirection.ASCENDING).asSnapshot()
            assertEquals(listOf("2317", "2330"), snapshot.map { it.code })
            verify(exactly = 1) { stockDao.observeAllAscendingPaged() }
            verify(exactly = 0) { stockDao.observeAllDescendingPaged() }
        }

    @Test
    fun `observeStocksPaged with descending direction uses descending DAO query`() =
        runTest {
            every {
                stockDao.observeAllDescendingPaged()
            } returns fakePagingSource(sampleEntity("2330"), sampleEntity("2317"))
            val snapshot = dataSource.observeStocksPaged(SortDirection.DESCENDING).asSnapshot()
            assertEquals(listOf("2330", "2317"), snapshot.map { it.code })
            verify(exactly = 1) { stockDao.observeAllDescendingPaged() }
            verify(exactly = 0) { stockDao.observeAllAscendingPaged() }
        }

    @Test
    fun `getStock returns entity returned by DAO`() =
        runTest {
            val expected = sampleEntity("2330")
            coEvery { stockDao.getByCode("2330") } returns expected
            assertEquals(expected, dataSource.getStock("2330"))
            coVerify(exactly = 1) { stockDao.getByCode("2330") }
        }

    @Test
    fun `getStock returns null when DAO finds no stock`() =
        runTest {
            coEvery { stockDao.getByCode("0000") } returns null
            assertNull(dataSource.getStock("0000"))
            coVerify(exactly = 1) { stockDao.getByCode("0000") }
        }

    @Test
    fun `observeMarketSummary returns DAO market summary flow`() =
        runTest {
            val expected = MarketSummaryRow(advancingCount = 100, decliningCount = 80, unchangedCount = 20)
            every { stockDao.observeMarketSummary() } returns flowOf(expected)
            assertEquals(expected, dataSource.observeMarketSummary().first())
            verify(exactly = 1) { stockDao.observeMarketSummary() }
        }

    @Test
    fun `observeLastRefreshedAt maps metadata to timestamp`() =
        runTest {
            // Stub the exact call the production code makes (default-arg call site compiles
            // to the same single-arg invocation), so the mock setup mirrors real usage.
            every { stockDao.observeRefreshMetadata() } returns
                flowOf(RefreshMetadataEntity(lastSuccessfulRefreshAt = 123_456_789L))
            assertEquals(123_456_789L, dataSource.observeLastRefreshedAt().first())
            verify(exactly = 1) { stockDao.observeRefreshMetadata() }
        }

    @Test
    fun `observeLastRefreshedAt emits null when metadata does not exist`() =
        runTest {
            every { stockDao.observeRefreshMetadata() } returns flowOf(null)
            assertNull(dataSource.observeLastRefreshedAt().first())
            verify(exactly = 1) { stockDao.observeRefreshMetadata() }
        }

    @Test
    fun `replaceAll delegates stocks and timestamp to DAO`() =
        runTest {
            val stocks = listOf(sampleEntity("2330"), sampleEntity("2317"))
            coEvery { stockDao.replaceAll(any(), any()) } returns Unit
            dataSource.replaceAll(stocks = stocks, refreshedAt = 123L)
            coVerify(exactly = 1) { stockDao.replaceAll(stocks = stocks, refreshedAt = 123L) }
        }

    private fun fakePagingSource(vararg entities: StockEntity): PagingSource<Int, StockEntity> =
        object : PagingSource<Int, StockEntity>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, StockEntity> =
                LoadResult.Page(data = entities.toList(), prevKey = null, nextKey = null)

            override fun getRefreshKey(state: PagingState<Int, StockEntity>): Int? = null
        }

    private fun sampleEntity(code: String) =
        StockEntity(
            code = code,
            name = "Stock $code",
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
