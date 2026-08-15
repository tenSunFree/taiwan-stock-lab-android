package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** 上市個股日成交資訊 */
@JsonClass(generateAdapter = true)
data class StockDayDto(
    @Json(name = "Code") val code: String? = null,
    @Json(name = "Name") val name: String? = null,
    @Json(name = "TradeVolume") val tradeVolume: String? = null,
    @Json(name = "TradeValue") val tradeValue: String? = null,
    @Json(name = "OpeningPrice") val openingPrice: String? = null,
    @Json(name = "HighestPrice") val highestPrice: String? = null,
    @Json(name = "LowestPrice") val lowestPrice: String? = null,
    @Json(name = "ClosingPrice") val closingPrice: String? = null,
    @Json(name = "Change") val change: String? = null,
    @Json(name = "Transaction") val transaction: String? = null,
)