package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract

import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel

data class StockListUiState(
    val stocks: List<StockUiModel> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
)