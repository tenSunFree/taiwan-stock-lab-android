package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.RefreshMetadataEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY code ASC")
    fun observeAll(): Flow<List<StockEntity>>

    @Query("SELECT * FROM refresh_metadata WHERE id = :id LIMIT 1")
    fun observeRefreshMetadata(id: Int = RefreshMetadataEntity.SINGLETON_ID): Flow<RefreshMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<StockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefreshMetadata(metadata: RefreshMetadataEntity)

    @Query("DELETE FROM stocks")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(
        stocks: List<StockEntity>,
        refreshedAt: Long,
    ) {
        clearAll()
        insertAll(stocks)
        insertRefreshMetadata(RefreshMetadataEntity(lastSuccessfulRefreshAt = refreshedAt))
    }
}
