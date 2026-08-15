package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.api.TwseApiService
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.model.TwseRawSnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TwseRemoteDataSource(
    private val api: TwseApiService,
) {
    suspend fun fetchSnapshot(): TwseRawSnapshot = coroutineScope {
        val valuationsDeferred = async { api.getStockValuations() }
        val dayAveragesDeferred = async { api.getStockDayAverages() }
        val daysDeferred = async { api.getStockDays() }
        TwseRawSnapshot(
            valuations = valuationsDeferred.await(),
            dayAverages = dayAveragesDeferred.await(),
            days = daysDeferred.await(),
        )
    }
}