package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao.StockDao
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity

@Database(entities = [StockEntity::class], version = 1, exportSchema = true)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
}