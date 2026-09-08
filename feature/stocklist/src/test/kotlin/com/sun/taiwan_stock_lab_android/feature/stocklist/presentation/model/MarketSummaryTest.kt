package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarketSummaryTest {
    @Test
    fun `totalCount returns sum of all market buckets`() {
        val summary = MarketSummary(advancingCount = 120, decliningCount = 45, unchangedCount = 8)
        assertEquals(173, summary.totalCount)
    }

    @Test
    fun `totalCount returns zero when all market buckets are zero`() {
        val summary = MarketSummary(advancingCount = 0, decliningCount = 0, unchangedCount = 0)
        assertEquals(0, summary.totalCount)
    }
}
