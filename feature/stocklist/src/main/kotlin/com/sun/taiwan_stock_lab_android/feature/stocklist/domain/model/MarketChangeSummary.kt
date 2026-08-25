package com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model

data class MarketChangeSummary(
    val advancingCount: Int,
    val decliningCount: Int,
    val unchangedCount: Int,
)
