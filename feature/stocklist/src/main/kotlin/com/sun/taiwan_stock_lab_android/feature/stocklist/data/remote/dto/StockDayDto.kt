package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** 上市個股日成交資訊 */
@JsonClass(generateAdapter = true)
data class StockDayDto(
    @param:Json(name = "Code") val code: String? = null,
    @param:Json(name = "Name") val name: String? = null,
    @param:Json(name = "TradeVolume") val tradeVolume: String? = null,
    @param:Json(name = "TradeValue") val tradeValue: String? = null,
    @param:Json(name = "OpeningPrice") val openingPrice: String? = null,
    @param:Json(name = "HighestPrice") val highestPrice: String? = null,
    @param:Json(name = "LowestPrice") val lowestPrice: String? = null,
    @param:Json(name = "ClosingPrice") val closingPrice: String? = null,
    @param:Json(name = "Change") val change: String? = null,
    @param:Json(name = "Transaction") val transaction: String? = null,
)
