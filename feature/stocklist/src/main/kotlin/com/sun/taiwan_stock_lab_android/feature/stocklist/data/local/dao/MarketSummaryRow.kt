package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.dao

/**
 * Result shape for [StockDao.observeMarketSummary]. Not a table-representing @Entity — just a
 * plain POJO whose constructor parameter names match the SELECT's column aliases, which Room
 * supports directly for query results.
 */
data class MarketSummaryRow(
    val advancingCount: Int,
    val decliningCount: Int,
    val unchangedCount: Int,
)
