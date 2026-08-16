package com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository

import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun observeStocks(): Flow<List<Stock>>
    fun observeLastRefreshedAt(): Flow<Long?>
    suspend fun refreshStocks(): Result<Unit>
}