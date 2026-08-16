package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation

import app.cash.turbine.test
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository.StockRepository
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.SortDirection
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

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cached stocks are exposed through ui state`() = runTest(testDispatcher) {
        val repository = createRepository(stocks = listOf(sampleStock2330))
        val viewModel = StockListViewModel(repository)
        advanceUntilIdle()
        assertEquals(listOf(sampleStock2330.toUiModel()), viewModel.uiState.value.stocks)
    }

    @Test
    fun `default sort direction is descending by code`() = runTest(testDispatcher) {
        val repository = createRepository(stocks = listOf(sampleStock0050, sampleStock2330))
        val viewModel = StockListViewModel(repository)
        advanceUntilIdle()
        assertEquals(SortDirection.DESCENDING, viewModel.uiState.value.sortDirection)
        assertEquals(listOf("2330", "0050"), viewModel.uiState.value.stocks.map { it.code })
    }

    @Test
    fun `selecting ascending sort reorders stocks by code`() = runTest(testDispatcher) {
        val repository = createRepository(stocks = listOf(sampleStock2330, sampleStock0050))
        val viewModel = StockListViewModel(repository)
        advanceUntilIdle()
        viewModel.onEvent(StockListUiEvent.OnSortDirectionSelected(SortDirection.ASCENDING))
        advanceUntilIdle()
        assertEquals(listOf("0050", "2330"), viewModel.uiState.value.stocks.map { it.code })
    }

    @Test
    fun `OnStart triggers refresh only once`() = runTest(testDispatcher) {
        val repository = createRepository()
        val viewModel = StockListViewModel(repository)
        viewModel.onEvent(StockListUiEvent.OnStart)
        viewModel.onEvent(StockListUiEvent.OnStart)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.refreshStocks() }
    }

    @Test
    fun `OnRefresh triggers a repository refresh`() = runTest(testDispatcher) {
        val repository = createRepository()
        val viewModel = StockListViewModel(repository)
        viewModel.onEvent(StockListUiEvent.OnRefresh)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.refreshStocks() }
        assertEquals(false, viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `refresh failure updates state and emits ShowError`() = runTest(testDispatcher) {
        val repository = mockk<StockRepository> {
            every { observeStocks() } returns flowOf(emptyList())
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
    fun `known stock click emits ShowStockDetail`() = runTest(testDispatcher) {
        val repository = createRepository(stocks = listOf(sampleStock2330))
        val viewModel = StockListViewModel(repository)
        advanceUntilIdle()
        viewModel.uiEffect.test {
            viewModel.onEvent(StockListUiEvent.OnStockClicked("2330"))
            assertEquals(
                StockListUiEffect.ShowStockDetail(sampleStock2330.toUiModel()),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unknown stock click emits nothing`() = runTest(testDispatcher) {
        val repository = createRepository(stocks = listOf(sampleStock2330))
        val viewModel = StockListViewModel(repository)
        advanceUntilIdle()
        viewModel.uiEffect.test {
            viewModel.onEvent(StockListUiEvent.OnStockClicked("9999"))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createRepository(stocks: List<Stock> = emptyList()): StockRepository =
        mockk {
            every { observeStocks() } returns flowOf(stocks)
            coEvery { refreshStocks() } returns Result.success(Unit)
        }

    private val sampleStock2330 = Stock(
        code = "2330", name = "台積電",
        openingPrice = null, highestPrice = null, lowestPrice = null, closingPrice = null,
        monthlyAveragePrice = null, change = null, tradeVolume = null, tradeValue = null,
        transactionCount = null, peRatio = null, dividendYield = null, pbRatio = null,
    )

    private val sampleStock0050 = sampleStock2330.copy(code = "0050", name = "元大台灣50")
}