package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** 上市個股日收盤價及月平均價 */
@JsonClass(generateAdapter = true)
data class StockDayAverageDto(
    @param:Json(name = "Code") val code: String? = null,
    @param:Json(name = "Name") val name: String? = null,
    @param:Json(name = "ClosingPrice") val closingPrice: String? = null,
    @param:Json(name = "MonthlyAveragePrice") val monthlyAveragePrice: String? = null,
)
