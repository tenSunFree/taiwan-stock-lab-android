package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val code: String,
    val name: String,
    val openingPrice: String?,
    val highestPrice: String?,
    val lowestPrice: String?,
    val closingPrice: String?,
    val monthlyAveragePrice: String?,
    val change: String?,
    val tradeVolume: Long?,
    val tradeValue: Long?,
    val transactionCount: Long?,
    val peRatio: String?,
    val dividendYield: String?,
    val pbRatio: String?,
)
