package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
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
    fun observeStocks(): Flow<List<StockEntity>> = stockDao.observeAll()

    fun observeLastRefreshedAt(): Flow<Long?> = stockDao.observeRefreshMetadata().map { it?.lastSuccessfulRefreshAt }

    suspend fun replaceAll(
        stocks: List<StockEntity>,
        refreshedAt: Long,
    ) = stockDao.replaceAll(stocks, refreshedAt)
}
