package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract

import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock

sealed interface StockListUiEffect {
    data class ShowStockDetail(val stock: Stock) : StockListUiEffect
    data class ShowError(val message: String) : StockListUiEffect
}