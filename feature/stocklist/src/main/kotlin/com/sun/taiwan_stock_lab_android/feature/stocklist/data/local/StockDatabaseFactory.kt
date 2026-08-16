package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

object StockDatabaseFactory {

    private const val DATABASE_NAME = "taiwan-stock-lab.db"

    fun create(context: Context): StockDatabase {
        val appContext = context.applicationContext
        val databaseFile = appContext.getDatabasePath(DATABASE_NAME)
        return Room.databaseBuilder<StockDatabase>(
            context = appContext,
            name = databaseFile.absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}