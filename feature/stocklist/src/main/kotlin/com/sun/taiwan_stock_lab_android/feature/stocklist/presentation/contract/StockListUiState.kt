package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract

import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock

data class StockListUiState(
    val stocks: List<Stock> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)