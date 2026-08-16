package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract

import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel

sealed interface StockListUiEffect {
    data class ShowStockDetail(val stock: StockUiModel) : StockListUiEffect
    data class ShowError(val message: String) : StockListUiEffect
}