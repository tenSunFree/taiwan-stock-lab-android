package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.api

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayAverageDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockValuationDto
import retrofit2.http.GET

interface TwseApiService {
    @GET("v1/exchangeReport/BWIBBU_ALL")
    suspend fun getStockValuations(): List<StockValuationDto>

    @GET("v1/exchangeReport/STOCK_DAY_AVG_ALL")
    suspend fun getStockDayAverages(): List<StockDayAverageDto>

    @GET("v1/exchangeReport/STOCK_DAY_ALL")
    suspend fun getStockDays(): List<StockDayDto>
}
