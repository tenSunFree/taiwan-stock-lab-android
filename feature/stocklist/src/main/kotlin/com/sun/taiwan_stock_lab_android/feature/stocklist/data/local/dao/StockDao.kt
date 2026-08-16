package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Query("SELECT * FROM stocks ORDER BY code ASC")
    fun observeAll(): Flow<List<StockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<StockEntity>)

    @Query("DELETE FROM stocks")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(stocks: List<StockEntity>) {
        clearAll()
        insertAll(stocks)
    }
}