package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.MarketChangeSummary
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.MarketSummary

// Counting advancing/declining/unchanged stocks now happens as a SQL aggregate
// (StockDao.observeMarketSummary) instead of classifying a fully-materialized List<StockUiModel>
// in Kotlin, so this is a trivial field mapping rather than a computation. Coverage for the
// actual counting logic now lives in a StockDao instrumentation test against the aggregate query.
fun MarketChangeSummary.toUiModel(): MarketSummary =
    MarketSummary(
        advancingCount = advancingCount,
        decliningCount = decliningCount,
        unchangedCount = unchangedCount,
    )
