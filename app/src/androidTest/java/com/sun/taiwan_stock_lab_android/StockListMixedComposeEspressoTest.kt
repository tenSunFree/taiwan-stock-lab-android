package com.sun.taiwan_stock_lab_android

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.compose.MarketSummaryBarTestTags
import com.sun.taiwan_stock_lab_android.util.waitUntilEspressoAssertionPasses
import com.sun.taiwan_stock_lab_android.util.withRecyclerViewItem
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.sun.taiwan_stock_lab_android.feature.stocklist.R as StockListR

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StockListMixedComposeEspressoTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun sortingViaEspresso_reordersRecyclerView_whileComposeSummaryStaysCorrect() {
        // Compose: verify FakeStockRepository's computed 1 advancing / 1 declining / 0 unchanged are rendered correctly
        composeTestRule.onNodeWithTag(MarketSummaryBarTestTags.ADVANCING).assertTextContains("1")
        // Default is descending; 2330 should be the first item
        composeTestRule.waitUntilEspressoAssertionPasses {
            onView(withId(R.id.recyclerViewStocks))
                .check(matches(withRecyclerViewItem(0, hasDescendant(withText("2330")))))
        }
        // Espresso: click the Toolbar sort menu to interact with the XML SortBottomSheetFragment
        onView(withId(R.id.action_sort)).perform(click())
        onView(withId(StockListR.id.radioSortAscending)).perform(click())
        // After switching to ascending, Room re-query + Paging diffing are asynchronous; poll until reorder completes
        composeTestRule.waitUntilEspressoAssertionPasses {
            onView(withId(R.id.recyclerViewStocks))
                .check(matches(withRecyclerViewItem(0, hasDescendant(withText("2317")))))
        }
        // Compose: sorting should not affect the MarketSummaryBar content
        composeTestRule.onNodeWithTag(MarketSummaryBarTestTags.ADVANCING).assertTextContains("1")
    }
}
