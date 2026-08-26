package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.MarketChangeSummary
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository.StockRepository
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEffect
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEvent
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper.toUiModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StockListViewModelTest {
    private val sampleStock2330 =
        Stock(
            code = "2330",
            name = "台積電",
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
    private val sampleStock0050 = sampleStock2330.copy(code = "0050", name = "元大台灣50")
    private val emptySummary = MarketChangeSummary(advancingCount = 0, decliningCount = 0, unchangedCount = 0)
    private val testDispatcher = StandardTestDispatcher()

    // asSnapshot() needs an explicit LoadStates signal to know a page finished loading;
    // PagingData.from(list) alone doesn't carry that, and stocksPagingData is built on top of a
    // MutableStateFlow (sortDirection) that never completes on its own, so without this,
    // asSnapshot() hangs waiting for a completion signal that never arrives — surfacing as
    // kotlinx.coroutines.test.UncompletedCoroutinesError when runTest tries to finish.
    private val fullyLoadedStates =
        LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = true),
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stocksPagingData reflects repository stocks for the default sort direction`() =
        runTest(testDispatcher) {
            // Sorting is now a query-level concern (see StockDao); the mock stands in for
            // "the DB already returned rows in descending order" rather than the ViewModel
            // sorting anything itself.
            val repository =
                createRepository(
                    descendingStocks = listOf(sampleStock2330, sampleStock0050),
                )
            val viewModel = StockListViewModel(repository)
            val snapshot = viewModel.stocksPagingData.asSnapshot()
            assertEquals(listOf(sampleStock2330.toUiModel(), sampleStock0050.toUiModel()), snapshot)
        }

    @Test
    fun `default sort direction is descending`() =
        runTest(testDispatcher) {
            val repository = createRepository()
            val viewModel = StockListViewModel(repository)
            advanceUntilIdle()
            assertEquals(SortDirection.DESCENDING, viewModel.uiState.value.sortDirection)
        }

    @Test
    fun `selecting ascending sort requests the ascending paged stream`() =
        runTest(testDispatcher) {
            val repository =
                createRepository(
                    descendingStocks = listOf(sampleStock2330, sampleStock0050),
                    ascendingStocks = listOf(sampleStock0050, sampleStock2330),
                )
            val viewModel = StockListViewModel(repository)
            advanceUntilIdle()
            viewModel.onEvent(StockListUiEvent.OnSortDirectionSelected(SortDirection.ASCENDING))
            advanceUntilIdle()
            assertEquals(SortDirection.ASCENDING, viewModel.uiState.value.sortDirection)
            val snapshot = viewModel.stocksPagingData.asSnapshot()
            assertEquals(listOf(sampleStock0050.toUiModel(), sampleStock2330.toUiModel()), snapshot)
        }

    @Test
    fun `selecting current sort direction is a no-op`() =
        runTest(testDispatcher) {
            val repository = createRepository()
            val viewModel = StockListViewModel(repository)
            advanceUntilIdle()
            viewModel.onEvent(StockListUiEvent.OnSortDirectionSelected(SortDirection.DESCENDING))
            advanceUntilIdle()
            // Only the initial subscription should have requested the descending stream.
            coVerify(exactly = 0) { repository.observeStocksPaged(SortDirection.ASCENDING) }
        }

    @Test
    fun `OnStart triggers refresh only once`() =
        runTest(testDispatcher) {
            val repository = createRepository()
            val viewModel = StockListViewModel(repository)
            viewModel.onEvent(StockListUiEvent.OnStart)
            viewModel.onEvent(StockListUiEvent.OnStart)
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.refreshStocks() }
        }

    @Test
    fun `OnRefresh triggers a repository refresh`() =
        runTest(testDispatcher) {
            val repository = createRepository()
            val viewModel = StockListViewModel(repository)
            viewModel.onEvent(StockListUiEvent.OnRefresh)
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.refreshStocks() }
            assertEquals(false, viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `refresh failure updates state and emits ShowError`() =
        runTest(testDispatcher) {
            val repository =
                mockk<StockRepository> {
                    every { observeStocksPaged(any()) } returns flowOf(PagingData.empty())
                    every { observeMarketSummary() } returns flowOf(emptySummary)
                    every { observeLastRefreshedAt() } returns flowOf(null)
                    coEvery { refreshStocks() } returns Result.failure(RuntimeException("Network failure"))
                }
            val viewModel = StockListViewModel(repository)
            viewModel.uiEffect.test {
                viewModel.onEvent(StockListUiEvent.OnRefresh)
                advanceUntilIdle()
                assertEquals(StockListUiEffect.ShowError("Network failure"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals("Network failure", viewModel.uiState.value.errorMessage)
            assertEquals(false, viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `known stock click emits ShowStockDetail`() =
        runTest(testDispatcher) {
            val repository = createRepository()
            coEvery { repository.getStock("2330") } returns sampleStock2330
            val viewModel = StockListViewModel(repository)
            advanceUntilIdle()
            viewModel.uiEffect.test {
                viewModel.onEvent(StockListUiEvent.OnStockClicked("2330"))
                assertEquals(
                    StockListUiEffect.ShowStockDetail(sampleStock2330.toUiModel()),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unknown stock click emits nothing`() =
        runTest(testDispatcher) {
            val repository = createRepository()
            coEvery { repository.getStock("9999") } returns null
            val viewModel = StockListViewModel(repository)
            advanceUntilIdle()
            viewModel.uiEffect.test {
                viewModel.onEvent(StockListUiEvent.OnStockClicked("9999"))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `market summary is exposed through ui state`() =
        runTest(testDispatcher) {
            val summary = MarketChangeSummary(advancingCount = 4, decliningCount = 2, unchangedCount = 1)
            val repository = createRepository(marketSummary = summary)
            val viewModel = StockListViewModel(repository)
            advanceUntilIdle()
            val uiSummary = viewModel.uiState.value.marketSummary
            assertEquals(4, uiSummary.advancingCount)
            assertEquals(2, uiSummary.decliningCount)
            assertEquals(1, uiSummary.unchangedCount)
        }

    @Test
    fun `last refreshed timestamp is exposed through ui state`() =
        runTest(testDispatcher) {
            val timestamp = 1_700_000_000_000L
            val repository = createRepository(lastRefreshedAt = timestamp)
            val viewModel = StockListViewModel(repository)
            advanceUntilIdle()
            assertEquals(timestamp, viewModel.uiState.value.lastUpdatedAt)
        }

    private fun createRepository(
        descendingStocks: List<Stock> = emptyList(),
        ascendingStocks: List<Stock> = emptyList(),
        marketSummary: MarketChangeSummary = emptySummary,
        lastRefreshedAt: Long? = null,
    ): StockRepository =
        mockk {
            every { observeStocksPaged(SortDirection.DESCENDING) } returns
                flowOf(PagingData.from(descendingStocks, sourceLoadStates = fullyLoadedStates))
            every { observeStocksPaged(SortDirection.ASCENDING) } returns
                flowOf(PagingData.from(ascendingStocks, sourceLoadStates = fullyLoadedStates))
            every { observeMarketSummary() } returns flowOf(marketSummary)
            every { observeLastRefreshedAt() } returns flowOf(lastRefreshedAt)
            coEvery { refreshStocks() } returns Result.success(Unit)
            coEvery { getStock(any()) } returns null
        }
}
