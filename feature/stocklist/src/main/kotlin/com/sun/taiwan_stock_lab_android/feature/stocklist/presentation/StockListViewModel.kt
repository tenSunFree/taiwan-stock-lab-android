package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository.StockRepository
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEffect
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEvent
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockListViewModel @Inject constructor(
    private val stockRepository: StockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockListUiState())
    val uiState: StateFlow<StockListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<StockListUiEffect>()
    val uiEffect: SharedFlow<StockListUiEffect> = _uiEffect.asSharedFlow()

    private var hasStarted = false
    private var refreshJob: Job? = null

    init {
        observeStocks()
    }

    fun onEvent(event: StockListUiEvent) {
        when (event) {
            StockListUiEvent.OnStart -> onStart()
            StockListUiEvent.OnRefresh -> refresh()
            is StockListUiEvent.OnStockClicked -> onStockClicked(event.stockCode)
        }
    }

    private fun observeStocks() {
        viewModelScope.launch {
            stockRepository.observeStocks().collect { stocks ->
                _uiState.update { it.copy(stocks = stocks) }
            }
        }
    }

    private fun onStart() {
        if (hasStarted) return
        hasStarted = true
        refresh()
    }

    private fun refresh() {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

            stockRepository.refreshStocks()
                .onSuccess {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: DEFAULT_REFRESH_ERROR_MESSAGE
                    _uiState.update { it.copy(isRefreshing = false, errorMessage = message) }
                    _uiEffect.emit(StockListUiEffect.ShowError(message))
                }
        }
    }

    private fun onStockClicked(stockCode: String) {
        val stock = _uiState.value.stocks.find { it.code == stockCode } ?: return
        viewModelScope.launch {
            _uiEffect.emit(StockListUiEffect.ShowStockDetail(stock))
        }
    }

    private companion object {
        const val DEFAULT_REFRESH_ERROR_MESSAGE = "Unable to refresh stock data."
    }
}