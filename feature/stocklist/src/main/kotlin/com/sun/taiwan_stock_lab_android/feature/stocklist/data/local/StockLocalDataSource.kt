package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.MarketSummaryRow
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Local source for stock cache and refresh metadata.
 *
 * Keeps Room-specific DAO access behind the data-source boundary so the
 * repository coordinates remote and local sources instead of depending
 * directly on Room DAOs.
 */
class StockLocalDataSource(
    private val stockDao: StockDao,
) {
    private companion object {
        const val PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 10
    }

    fun observeStocksPaged(direction: SortDirection): Flow<PagingData<StockEntity>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = PAGE_SIZE,
                    prefetchDistance = PREFETCH_DISTANCE,
                    enablePlaceholders = false,
                ),
            pagingSourceFactory = {
                when (direction) {
                    SortDirection.ASCENDING -> stockDao.observeAllAscendingPaged()
                    SortDirection.DESCENDING -> stockDao.observeAllDescendingPaged()
                }
            },
        ).flow

    suspend fun getStock(code: String): StockEntity? = stockDao.getByCode(code)

    fun observeMarketSummary(): Flow<MarketSummaryRow> = stockDao.observeMarketSummary()

    fun observeLastRefreshedAt(): Flow<Long?> = stockDao.observeRefreshMetadata().map { it?.lastSuccessfulRefreshAt }

    suspend fun replaceAll(
        stocks: List<StockEntity>,
        refreshedAt: Long,
    ) = stockDao.replaceAll(stocks, refreshedAt)
}
