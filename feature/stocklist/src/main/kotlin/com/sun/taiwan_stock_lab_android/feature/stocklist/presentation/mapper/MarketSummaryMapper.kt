package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.ChangeDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.MarketSummary
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel

fun computeMarketSummary(stocks: List<StockUiModel>): MarketSummary {
    var advancing = 0
    var declining = 0
    var unchanged = 0
    for (stock in stocks) {
        when (stock.changeDirection) {
            ChangeDirection.POSITIVE -> advancing++
            ChangeDirection.NEGATIVE -> declining++
            ChangeDirection.ZERO, ChangeDirection.UNKNOWN -> unchanged++
        }
    }
    return MarketSummary(
        advancingCount = advancing,
        decliningCount = declining,
        unchangedCount = unchanged,
    )
}