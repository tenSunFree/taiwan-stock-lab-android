package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository.StockRepository
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEffect
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEvent
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiState
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper.toUiModel
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StockListViewModel
    @Inject
    constructor(
        private val stockRepository: StockRepository,
    ) : ViewModel() {
        private companion object {
            const val DEFAULT_REFRESH_ERROR_MESSAGE = "Unable to refresh stock data."
        }

        private val _uiState = MutableStateFlow(StockListUiState())
        val uiState: StateFlow<StockListUiState> = _uiState.asStateFlow()

        private val _uiEffect = MutableSharedFlow<StockListUiEffect>()
        val uiEffect: SharedFlow<StockListUiEffect> = _uiEffect.asSharedFlow()

        // Drives the paged stream below. Kept separate from _uiState.sortDirection (which mirrors
        // it for UI display) because flatMapLatest needs its own Flow to react to changes.
        private val sortDirection = MutableStateFlow(SortDirection.DESCENDING)

        // The RecyclerView's list content. Deliberately not part of StockListUiState — PagingData
        // is single-consumer and isn't meaningfully comparable, so it's exposed as its own Flow
        // for a PagingDataAdapter to collect directly, rather than folded into a data class.
        // flatMapLatest re-subscribes to a fresh Pager whenever sortDirection changes, so a sort
        // change becomes a query-level re-fetch (ORDER BY ASC/DESC) rather than an in-memory
        // re-sort of an already-loaded list.
        val stocksPagingData: Flow<PagingData<StockUiModel>> =
            sortDirection
                .flatMapLatest { direction -> stockRepository.observeStocksPaged(direction) }
                .map { pagingData -> pagingData.map { it.toUiModel() } }
                .cachedIn(viewModelScope)

        private var hasStarted = false
        private var refreshJob: Job? = null

        init {
            observeMarketSummary()
            observeLastRefreshedAt()
        }

        private fun observeMarketSummary() {
            viewModelScope.launch {
                stockRepository.observeMarketSummary().collect { summary ->
                    _uiState.update { it.copy(marketSummary = summary.toUiModel()) }
                }
            }
        }

        private fun observeLastRefreshedAt() {
            viewModelScope.launch {
                stockRepository.observeLastRefreshedAt().collect { timestamp ->
                    _uiState.update { it.copy(lastUpdatedAt = timestamp) }
                }
            }
        }

        fun onEvent(event: StockListUiEvent) {
            when (event) {
                StockListUiEvent.OnStart -> onStart()
                StockListUiEvent.OnRefresh -> refresh()
                is StockListUiEvent.OnStockClicked -> onStockClicked(event.stockCode)
                is StockListUiEvent.OnSortDirectionSelected -> onSortDirectionSelected(event.direction)
            }
        }

        private fun onSortDirectionSelected(direction: SortDirection) {
            if (direction == sortDirection.value) return
            sortDirection.value = direction
            _uiState.update { it.copy(sortDirection = direction) }
        }

        private fun onStart() {
            if (hasStarted) return
            hasStarted = true
            refresh()
        }

        private fun refresh() {
            if (refreshJob?.isActive == true) return
            refreshJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                    stockRepository
                        .refreshStocks()
                        .onSuccess {
                            _uiState.update { it.copy(isRefreshing = false) }
                        }.onFailure { throwable ->
                            val message = throwable.message ?: DEFAULT_REFRESH_ERROR_MESSAGE
                            _uiState.update { it.copy(isRefreshing = false, errorMessage = message) }
                            _uiEffect.emit(StockListUiEffect.ShowError(message))
                        }
                }
        }

        // Single-row lookup rather than scanning a full in-memory stock list — the event
        // intentionally carries just the stock code (not a full StockUiModel), keeping the MVI
        // event contract as "what the user did", not "here's the data to display".
        private fun onStockClicked(stockCode: String) {
            viewModelScope.launch {
                val stock = stockRepository.getStock(stockCode) ?: return@launch
                _uiEffect.emit(StockListUiEffect.ShowStockDetail(stock.toUiModel()))
            }
        }
    }
