package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** 上市個股日本益比、殖利率及股價淨值比 */
@JsonClass(generateAdapter = true)
data class StockValuationDto(
    @Json(name = "Code") val code: String? = null,
    @Json(name = "Name") val name: String? = null,
    @Json(name = "PEratio") val peRatio: String? = null,
    @Json(name = "DividendYield") val dividendYield: String? = null,
    @Json(name = "PBratio") val pbRatio: String? = null,
)