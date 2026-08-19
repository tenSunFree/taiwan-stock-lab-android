package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model

data class StockUiModel(
    val code: String,
    val name: String,
    val openingPrice: String,
    val highestPrice: String,
    val lowestPrice: String,
    val closingPrice: String,
    val monthlyAveragePrice: String,
    val change: String,
    val tradeVolume: String,
    val tradeValue: String,
    val transactionCount: String,
    val peRatio: String,
    val dividendYield: String,
    val pbRatio: String,
    val closingPricePosition: PricePosition,
    val changeDirection: ChangeDirection,
)
