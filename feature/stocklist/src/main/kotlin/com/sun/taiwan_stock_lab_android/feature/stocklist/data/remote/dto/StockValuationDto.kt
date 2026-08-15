package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** 上市個股日本益比、殖利率及股價淨值比 */
@JsonClass(generateAdapter = true)
data class StockValuationDto(
    @param:Json(name = "Code") val code: String? = null,
    @param:Json(name = "Name") val name: String? = null,
    @param:Json(name = "PEratio") val peRatio: String? = null,
    @param:Json(name = "DividendYield") val dividendYield: String? = null,
    @param:Json(name = "PBratio") val pbRatio: String? = null,
)