package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract

sealed interface StockListUiEvent {
    data object OnStart : StockListUiEvent
    data object OnRefresh : StockListUiEvent
    data class OnStockClicked(val stockCode: String) : StockListUiEvent
    data class OnSortDirectionSelected(val direction: SortDirection) : StockListUiEvent
}