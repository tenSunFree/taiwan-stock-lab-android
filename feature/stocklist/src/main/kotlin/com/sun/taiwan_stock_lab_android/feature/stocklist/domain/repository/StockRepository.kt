package com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository

import androidx.paging.PagingData
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.MarketChangeSummary
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    // Paged stream for RecyclerView rendering. Sort direction is a query-level concern here
    // (ORDER BY ASC/DESC), not an in-memory re-sort — see StockLocalDataSource.
    fun observeStocksPaged(direction: SortDirection): Flow<PagingData<Stock>>

    // Single-row lookup, not a full-list scan — used to resolve a clicked stock code to its
    // full model for the detail dialog.
    suspend fun getStock(code: String): Stock?

    // DB-side aggregate (SUM/CASE over the `change` column) rather than streaming every row into
    // memory just to count advancing/declining/unchanged — see StockDao.observeMarketSummary.
    fun observeMarketSummary(): Flow<MarketChangeSummary>

    fun observeLastRefreshedAt(): Flow<Long?>

    suspend fun refreshStocks(): Result<Unit>
}
