package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.model

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayAverageDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockValuationDto

data class TwseRawSnapshot(
    val valuations: List<StockValuationDto>,
    val dayAverages: List<StockDayAverageDto>,
    val days: List<StockDayDto>,
)