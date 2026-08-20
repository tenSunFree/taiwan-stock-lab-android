package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.RefreshMetadataEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity

@Database(
    entities = [StockEntity::class, RefreshMetadataEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
}
