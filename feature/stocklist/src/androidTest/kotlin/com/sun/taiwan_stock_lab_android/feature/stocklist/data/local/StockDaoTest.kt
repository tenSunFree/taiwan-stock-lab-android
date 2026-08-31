package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local

import androidx.paging.PagingSource
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockDaoTest {
    private companion object {
        const val DEFAULT_LOAD_SIZE = 20
    }

    private lateinit var database: StockDatabase
    private lateinit var dao: StockDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database =
            Room
                .inMemoryDatabaseBuilder<StockDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()
        dao = database.stockDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // replaceAll / refresh metadata. These verify successful-path consistency between the stock
    // cache and refresh_metadata, not transactional rollback under a mid-write failure — no test
    // here deliberately triggers a failure partway through replaceAll() and asserts the DB is
    // left unchanged, so "transactional replacement" is the accurate claim, not "atomicity".
    @Test
    fun replaceAll_clearsPreviousDataAndInsertsNew() =
        runBlocking {
            dao.replaceAll(listOf(sampleEntity("2330")), refreshedAt = 1L)
            dao.replaceAll(listOf(sampleEntity("0050")), refreshedAt = 2L)
            val page = loadRefresh(dao.observeAllAscendingPaged())
            assertEquals(1, page.data.size)
            assertEquals("0050", page.data.single().code)
        }

    @Test
    fun replaceAll_writesRefreshMetadataAlongsideStocks() =
        runBlocking {
            val refreshedAt = 1_700_000_000_000L
            dao.replaceAll(listOf(sampleEntity("2330")), refreshedAt = refreshedAt)
            val metadata = dao.observeRefreshMetadata().first()
            assertEquals(refreshedAt, metadata?.lastSuccessfulRefreshAt)
        }

    @Test
    fun replaceAll_replacesStocksAndRefreshMetadataTogether() =
        runBlocking {
            dao.replaceAll(listOf(sampleEntity("2330")), refreshedAt = 100L)
            dao.replaceAll(listOf(sampleEntity("0050")), refreshedAt = 200L)
            val page = loadRefresh(dao.observeAllAscendingPaged())
            val metadata = dao.observeRefreshMetadata().first()
            assertEquals(listOf("0050"), page.data.map { it.code })
            assertEquals(200L, metadata?.lastSuccessfulRefreshAt)
        }

    // Paged queries — Refresh + Append, exercising the actual PagingSource contract rather than
    // treating it as a one-shot "load everything" API.
    @Test
    fun observeAllAscendingPaged_loadsStocksOrderedAscendingByCode() =
        runBlocking {
            dao.insertAll(listOf(sampleEntity("2330"), sampleEntity("0050"), sampleEntity("1101")))
            val page = loadRefresh(dao.observeAllAscendingPaged())
            assertEquals(listOf("0050", "1101", "2330"), page.data.map { it.code })
        }

    @Test
    fun observeAllDescendingPaged_loadsStocksOrderedDescendingByCode() =
        runBlocking {
            dao.insertAll(listOf(sampleEntity("2330"), sampleEntity("0050"), sampleEntity("1101")))
            val page = loadRefresh(dao.observeAllDescendingPaged())
            assertEquals(listOf("2330", "1101", "0050"), page.data.map { it.code })
        }

    @Test
    fun observeAllAscendingPaged_returnsEmptyPageWhenNoStocksExist() =
        runBlocking {
            val page = loadRefresh(dao.observeAllAscendingPaged())
            assertTrue(page.data.isEmpty())
        }

    @Test
    fun observeAllAscendingPaged_loadsSubsequentPageViaAppend() =
        runBlocking {
            dao.insertAll(
                listOf("0001", "0002", "0003", "0004", "0005").map { sampleEntity(it) },
            )
            val pagingSource = dao.observeAllAscendingPaged()
            val firstPage = loadRefresh(pagingSource, loadSize = 2)
            assertEquals(listOf("0001", "0002"), firstPage.data.map { it.code })

            val nextKey = requireNotNull(firstPage.nextKey)
            val secondResult =
                pagingSource.load(
                    PagingSource.LoadParams.Append(key = nextKey, loadSize = 2, placeholdersEnabled = false),
                )
            assertTrue(secondResult is PagingSource.LoadResult.Page)
            @Suppress("UNCHECKED_CAST")
            val secondPage = secondResult as PagingSource.LoadResult.Page<Int, StockEntity>
            assertEquals(listOf("0003", "0004"), secondPage.data.map { it.code })
        }

    // Single-row lookup
    @Test
    fun getByCode_returnsMatchingStock() =
        runBlocking {
            dao.insertAll(listOf(sampleEntity("2330"), sampleEntity("0050")))
            val stock = dao.getByCode("2330")
            assertEquals("2330", stock?.code)
        }

    @Test
    fun getByCode_returnsNullWhenNoStockMatches() =
        runBlocking {
            dao.insertAll(listOf(sampleEntity("2330")))
            val stock = dao.getByCode("9999")
            assertNull(stock)
        }

    // Market summary aggregate — the part most at risk of getting the NULL/zero bucketing wrong,
    // since the SQL CAST-based classification has to match the Kotlin ChangeDirection rule that
    // ZERO and UNKNOWN (unparseable/missing) both count as "unchanged".
    @Test
    fun observeMarketSummary_countsPositiveChangeAsAdvancing() =
        runBlocking {
            dao.insertAll(listOf(sampleEntity("2330", change = "1.5")))
            val summary = dao.observeMarketSummary().first()
            assertEquals(1, summary.advancingCount)
            assertEquals(0, summary.decliningCount)
            assertEquals(0, summary.unchangedCount)
        }

    @Test
    fun observeMarketSummary_countsNegativeChangeAsDeclining() =
        runBlocking {
            dao.insertAll(listOf(sampleEntity("2330", change = "-0.25")))
            val summary = dao.observeMarketSummary().first()
            assertEquals(0, summary.advancingCount)
            assertEquals(1, summary.decliningCount)
            assertEquals(0, summary.unchangedCount)
        }

    @Test
    fun observeMarketSummary_countsZeroChangeAsUnchanged() =
        runBlocking {
            dao.insertAll(listOf(sampleEntity("2330", change = "0")))
            val summary = dao.observeMarketSummary().first()
            assertEquals(0, summary.advancingCount)
            assertEquals(0, summary.decliningCount)
            assertEquals(1, summary.unchangedCount)
        }

    // The critical case called out in the Roadmap: a NULL `change` column (a stock with no
    // parseable change value — see TwseNumericParser) must be bucketed as "unchanged", the same
    // as a real zero, matching ChangeDirection.UNKNOWN's grouping in the original Kotlin
    // implementation. Under standard SQL three-valued logic, `NULL = 0` evaluates to NULL, not
    // TRUE — the query has to test `change IS NULL` explicitly rather than relying on `= 0` to
    // catch it, and this test verifies that it does.
    @Test
    fun observeMarketSummary_countsNullChangeAsUnchanged() =
        runBlocking {
            dao.insertAll(listOf(sampleEntity("2330", change = null)))
            val summary = dao.observeMarketSummary().first()
            assertEquals(0, summary.advancingCount)
            assertEquals(0, summary.decliningCount)
            assertEquals(1, summary.unchangedCount)
        }

    @Test
    fun observeMarketSummary_countsMixedStocksAcrossAllBuckets() =
        runBlocking {
            dao.insertAll(
                listOf(
                    sampleEntity("0001", change = "2.0"),
                    sampleEntity("0002", change = "0.5"),
                    sampleEntity("0003", change = "-1.0"),
                    sampleEntity("0004", change = "0"),
                    sampleEntity("0005", change = null),
                ),
            )
            val summary = dao.observeMarketSummary().first()
            assertEquals(2, summary.advancingCount)
            assertEquals(1, summary.decliningCount)
            assertEquals(2, summary.unchangedCount)
        }

    @Test
    fun observeMarketSummary_returnsAllZeroForEmptyTable() =
        runBlocking {
            val summary = dao.observeMarketSummary().first()
            assertEquals(0, summary.advancingCount)
            assertEquals(0, summary.decliningCount)
            assertEquals(0, summary.unchangedCount)
        }

    private suspend fun loadRefresh(
        pagingSource: PagingSource<Int, StockEntity>,
        loadSize: Int = DEFAULT_LOAD_SIZE,
    ): PagingSource.LoadResult.Page<Int, StockEntity> {
        val result =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = loadSize, placeholdersEnabled = false),
            )
        assertTrue(result is PagingSource.LoadResult.Page)
        @Suppress("UNCHECKED_CAST")
        return result as PagingSource.LoadResult.Page<Int, StockEntity>
    }

    private fun sampleEntity(
        code: String,
        change: String? = null,
    ) = StockEntity(
        code = code,
        name = "Test",
        openingPrice = null,
        highestPrice = null,
        lowestPrice = null,
        closingPrice = null,
        monthlyAveragePrice = null,
        change = change,
        tradeVolume = null,
        tradeValue = null,
        transactionCount = null,
        peRatio = null,
        dividendYield = null,
        pbRatio = null,
    )
}
