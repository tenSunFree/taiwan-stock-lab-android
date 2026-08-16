package com.sun.taiwan_stock_lab_android.feature.stocklist.data.repository

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.mapper.StockMapper
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.mapper.toDomain
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.mapper.toEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.TwseRemoteDataSource
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository.StockRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room is the single source of truth. [observeStocks] always reads from the local
 * database; [refreshStocks] fetches from the network and writes the result into Room.
 * On refresh failure — or an empty/invalid remote snapshot — the existing cache is
 * left untouched.
 */
class OfflineFirstStockRepository(
    private val remoteDataSource: TwseRemoteDataSource,
    private val stockDao: StockDao,
) : StockRepository {

    override fun observeStocks(): Flow<List<Stock>> =
        stockDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshStocks(): Result<Unit> {
        try {
            val snapshot = remoteDataSource.fetchSnapshot()
            val stocks = StockMapper.merge(snapshot)

            if (stocks.isEmpty()) {
                return Result.failure(EmptyStockSnapshotException())
            }

            stockDao.replaceAll(stocks.map { it.toEntity() })
            return Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }
}