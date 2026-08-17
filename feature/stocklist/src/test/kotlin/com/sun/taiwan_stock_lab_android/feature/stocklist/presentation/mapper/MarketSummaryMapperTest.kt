package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.ChangeDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.PricePosition
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarketSummaryMapperTest {

    @Test
    fun `computeMarketSummary counts stocks by change direction`() {
        val stocks = listOf(
            stockWith(ChangeDirection.POSITIVE),
            stockWith(ChangeDirection.POSITIVE),
            stockWith(ChangeDirection.NEGATIVE),
            stockWith(ChangeDirection.ZERO),
            stockWith(ChangeDirection.UNKNOWN),
        )
        val summary = computeMarketSummary(stocks)
        assertEquals(2, summary.advancingCount)
        assertEquals(1, summary.decliningCount)
        assertEquals(2, summary.unchangedCount)
        assertEquals(5, summary.totalCount)
    }

    @Test
    fun `computeMarketSummary returns all zero for empty list`() {
        val summary = computeMarketSummary(emptyList())
        assertEquals(0, summary.advancingCount)
        assertEquals(0, summary.decliningCount)
        assertEquals(0, summary.unchangedCount)
        assertEquals(0, summary.totalCount)
    }

    private fun stockWith(direction: ChangeDirection) = StockUiModel(
        code = "0000", name = "Test",
        openingPrice = "--", highestPrice = "--", lowestPrice = "--", closingPrice = "--",
        monthlyAveragePrice = "--", change = "--", tradeVolume = "--", tradeValue = "--",
        transactionCount = "--", peRatio = "--", dividendYield = "--", pbRatio = "--",
        closingPricePosition = PricePosition.UNKNOWN, changeDirection = direction,
    )
}