package com.sun.taiwan_stock_lab_android.feature.stocklist.data.repository

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.StockLocalDataSource
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
 * Offline-first repository. Room remains the single source of truth.
 * Refreshes fetch a complete remote snapshot and atomically replace the
 * local cache together with the successful refresh timestamp.
 */
class OfflineFirstStockRepository(
    private val remoteDataSource: TwseRemoteDataSource,
    private val localDataSource: StockLocalDataSource,
    private val clock: () -> Long = System::currentTimeMillis,
) : StockRepository {

    override fun observeStocks(): Flow<List<Stock>> =
        localDataSource.observeStocks().map { entities -> entities.map { it.toDomain() } }

    override fun observeLastRefreshedAt(): Flow<Long?> =
        localDataSource.observeLastRefreshedAt()

    override suspend fun refreshStocks(): Result<Unit> =
        try {
            val snapshot = remoteDataSource.fetchSnapshot()
            val stocks = StockMapper.merge(snapshot)
            if (stocks.isEmpty()) {
                Result.failure(EmptyStockSnapshotException())
            } else {
                localDataSource.replaceAll(stocks.map { it.toEntity() }, refreshedAt = clock())
                Result.success(Unit)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
}