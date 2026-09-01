package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.compose

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sun.taiwan_stock_lab_android.core.ui.theme.StockLabTheme
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.MarketSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarketSummaryBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun marketSummaryBar_displaysMarketCounts() {
        composeTestRule.setContent {
            StockLabTheme {
                MarketSummaryBar(
                    summary = MarketSummary(advancingCount = 812, decliningCount = 431, unchangedCount = 57),
                )
            }
        }
        composeTestRule
            .onNodeWithTag(MarketSummaryBarTestTags.ADVANCING)
            .assertTextContains("上漲")
            .assertTextContains("812")
        composeTestRule
            .onNodeWithTag(MarketSummaryBarTestTags.DECLINING)
            .assertTextContains("下跌")
            .assertTextContains("431")
        composeTestRule
            .onNodeWithTag(MarketSummaryBarTestTags.UNCHANGED)
            .assertTextContains("平盤")
            .assertTextContains("57")
    }
}
