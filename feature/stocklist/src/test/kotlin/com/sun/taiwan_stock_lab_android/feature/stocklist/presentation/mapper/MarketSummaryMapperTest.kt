package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.MarketChangeSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// The actual advancing/declining/unchanged counting logic moved to a SQL aggregate
// (StockDao.observeMarketSummary), so this only needs to verify the trivial field mapping.
// Coverage for the counting logic itself — including how a NULL `change` column is bucketed —
// now lives in StockDaoTest.
class MarketSummaryMapperTest {
    @Test
    fun `toUiModel maps fields across unchanged`() {
        val domainSummary =
            MarketChangeSummary(
                advancingCount = 12,
                decliningCount = 7,
                unchangedCount = 3,
            )
        val uiSummary = domainSummary.toUiModel()
        assertEquals(12, uiSummary.advancingCount)
        assertEquals(7, uiSummary.decliningCount)
        assertEquals(3, uiSummary.unchangedCount)
        assertEquals(22, uiSummary.totalCount)
    }

    @Test
    fun `toUiModel maps all zero summary`() {
        val domainSummary = MarketChangeSummary(advancingCount = 0, decliningCount = 0, unchangedCount = 0)
        val uiSummary = domainSummary.toUiModel()
        assertEquals(0, uiSummary.totalCount)
    }
}
