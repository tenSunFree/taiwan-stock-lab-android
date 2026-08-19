package com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model

import java.math.BigDecimal

data class Stock(
    val code: String,
    val name: String,
    val openingPrice: BigDecimal?,
    val highestPrice: BigDecimal?,
    val lowestPrice: BigDecimal?,
    val closingPrice: BigDecimal?,
    val monthlyAveragePrice: BigDecimal?,
    val change: BigDecimal?,
    val tradeVolume: Long?,
    val tradeValue: Long?,
    val transactionCount: Long?,
    val peRatio: BigDecimal?,
    val dividendYield: BigDecimal?,
    val pbRatio: BigDecimal?,
)
