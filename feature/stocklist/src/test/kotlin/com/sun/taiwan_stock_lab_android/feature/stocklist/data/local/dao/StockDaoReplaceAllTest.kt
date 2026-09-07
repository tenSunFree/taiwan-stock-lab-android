package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.RefreshMetadataEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * StockDao.replaceAll is a Kotlin interface method with a real body (a @Transaction
 * clear -> insert -> stamp-timestamp sequence), not a Room-generated query. Room's actual
 * generated implementation is only exercised by StockDaoTest (androidTest), which never
 * contributes to the JVM coverage report. This fake lets a plain JUnit test verify the
 * *ordering invariant* this method exists to guarantee: the cache is only considered
 * "refreshed" after both the stock rows and the timestamp have been written together.
 */
class StockDaoReplaceAllTest {
    private class FakeStockDao : StockDao {
        val calls = mutableListOf<String>()

        var storedStocks: List<StockEntity> = emptyList()
            private set

        var storedMetadata: RefreshMetadataEntity? = null
            private set

        override fun observeAllAscendingPaged(): PagingSource<Int, StockEntity> = emptyPagingSource()

        override fun observeAllDescendingPaged(): PagingSource<Int, StockEntity> = emptyPagingSource()

        override suspend fun getByCode(code: String): StockEntity? = storedStocks.find { it.code == code }

        override fun observeMarketSummary(): Flow<MarketSummaryRow> =
            MutableStateFlow(MarketSummaryRow(advancingCount = 0, decliningCount = 0, unchangedCount = 0))

        override fun observeRefreshMetadata(id: Int): Flow<RefreshMetadataEntity?> = MutableStateFlow(storedMetadata)

        override suspend fun insertAll(stocks: List<StockEntity>) {
            calls += "insertAll"
            storedStocks = stocks
        }

        override suspend fun insertRefreshMetadata(metadata: RefreshMetadataEntity) {
            calls += "insertRefreshMetadata"
            storedMetadata = metadata
        }

        override suspend fun clearAll() {
            calls += "clearAll"
            storedStocks = emptyList()
        }

        private fun emptyPagingSource(): PagingSource<Int, StockEntity> =
            object : PagingSource<Int, StockEntity>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, StockEntity> =
                    LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)

                override fun getRefreshKey(state: PagingState<Int, StockEntity>): Int? = null
            }
    }

    @Test
    fun `replaceAll clears old stocks, inserts new stocks, then updates metadata in order`() =
        runTest {
            val dao = FakeStockDao()
            val stocks = listOf(sampleEntity("2330"), sampleEntity("2317"))
            dao.replaceAll(stocks = stocks, refreshedAt = 123L)
            assertEquals(listOf("clearAll", "insertAll", "insertRefreshMetadata"), dao.calls)
            assertEquals(stocks, dao.storedStocks)
            assertEquals(123L, dao.storedMetadata?.lastSuccessfulRefreshAt)
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
