package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model

data class MarketSummary(
    val advancingCount: Int,
    val decliningCount: Int,
    val unchangedCount: Int,
) {
    val totalCount: Int get() = advancingCount + decliningCount + unchangedCount
}
