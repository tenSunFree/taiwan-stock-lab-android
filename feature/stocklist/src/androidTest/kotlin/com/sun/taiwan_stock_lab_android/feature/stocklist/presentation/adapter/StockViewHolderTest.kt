package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.adapter

import android.app.Application
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sun.taiwan_stock_lab_android.feature.stocklist.R
import com.sun.taiwan_stock_lab_android.feature.stocklist.databinding.ItemStockCardBinding
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.ChangeDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.PricePosition
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.google.android.material.R as MaterialR
import com.sun.taiwan_stock_lab_android.core.ui.R as CoreUiR

/**
 * Instrumented (androidTest) counterpart of the StockViewHolder unit test that was attempted
 * under Robolectric. Moved here after confirming — with a minimal repro (RecyclerViewSanityTest)
 * and multiple isolated attempts — that constructing a themed ViewBinding-inflated View in this
 * module's Robolectric setup reliably fails with "Method getContext in android.view.View not
 * mocked", unrelated to JaCoCo/ASM instrumentation (confirmed absent via --info log inspection).
 * Running on a real Android runtime sidesteps that environment issue entirely and is at least as
 * trustworthy a signal for real View/resource behavior.
 *
 * Does not construct StockListAdapter (see RecyclerViewSanityTest for why that specifically NPEs
 * under Robolectric) — StockListAdapter's own onCreateViewHolder/onBindViewHolder wiring through
 * the real adapter is covered by StockListEspressoTest.
 */
@RunWith(AndroidJUnit4::class)
class StockViewHolderTest {
    private var clickedCode: String? = null

    private lateinit var holder: StockListAdapter.StockViewHolder

    @Before
    fun setUp() {
        clickedCode = null
        val context = ApplicationProvider.getApplicationContext<Application>()
        // MaterialCardView (used by item_stock_card.xml) enforces at construction time that the
        // Context's theme is Theme.MaterialComponents/Theme.Material3 or a descendant. The bare
        // Application context has no such theme applied, so wrap it explicitly — this is a
        // standard Material Components requirement, not an instrumentation/emulator quirk.
        val themedContext =
            ContextThemeWrapper(context, MaterialR.style.Theme_Material3_DayNight_NoActionBar)
        val parent = FrameLayout(themedContext)
        val binding = ItemStockCardBinding.inflate(LayoutInflater.from(themedContext), parent, false)
        holder = StockListAdapter.StockViewHolder(binding, onStockClicked = { clickedCode = it })
    }

    private fun textOf(id: Int): String =
        holder.itemView
            .findViewById<TextView>(id)
            .text
            .toString()

    private fun colorOf(resId: Int): Int = ContextCompat.getColor(holder.itemView.context, resId)

    private fun closingPriceColor(): Int =
        holder.itemView.findViewById<TextView>(R.id.textClosingPrice).currentTextColor

    private fun changeColor(): Int = holder.itemView.findViewById<TextView>(R.id.textChange).currentTextColor

    private fun sampleStock(
        code: String = "2330",
        closingPricePosition: PricePosition = PricePosition.EQUAL,
        changeDirection: ChangeDirection = ChangeDirection.ZERO,
    ) = StockUiModel(
        code = code,
        name = "台積電",
        openingPrice = "500.0",
        highestPrice = "510.0",
        lowestPrice = "495.0",
        closingPrice = "505.0",
        monthlyAveragePrice = "498.0",
        change = "+5.0",
        tradeVolume = "10000",
        tradeValue = "5000000",
        transactionCount = "300",
        peRatio = "15.0",
        dividendYield = "2.5",
        pbRatio = "3.0",
        closingPricePosition = closingPricePosition,
        changeDirection = changeDirection,
    )

    @Test
    fun bind_populatesAllCardTextFieldsFromModel() {
        val stock = sampleStock()
        holder.bind(stock)
        assertEquals(stock.code, textOf(R.id.textCode))
        assertEquals(stock.name, textOf(R.id.textName))
        assertEquals(stock.openingPrice, textOf(R.id.textOpeningPrice))
        assertEquals(stock.highestPrice, textOf(R.id.textHighestPrice))
        assertEquals(stock.lowestPrice, textOf(R.id.textLowestPrice))
        assertEquals(stock.monthlyAveragePrice, textOf(R.id.textMonthlyAverage))
        assertEquals(stock.transactionCount, textOf(R.id.textTransactionCount))
        assertEquals(stock.tradeVolume, textOf(R.id.textTradeVolume))
        assertEquals(stock.tradeValue, textOf(R.id.textTradeValue))
        assertEquals(stock.closingPrice, textOf(R.id.textClosingPrice))
        assertEquals(stock.change, textOf(R.id.textChange))
    }

    @Test
    fun bind_aboveAveragePosition_colorsClosingPriceUp() {
        holder.bind(sampleStock(closingPricePosition = PricePosition.ABOVE_AVERAGE))
        assertEquals(colorOf(CoreUiR.color.stock_price_up), closingPriceColor())
    }

    @Test
    fun bind_belowAveragePosition_colorsClosingPriceDown() {
        holder.bind(sampleStock(closingPricePosition = PricePosition.BELOW_AVERAGE))
        assertEquals(colorOf(CoreUiR.color.stock_price_down), closingPriceColor())
    }

    @Test
    fun bind_equalPosition_colorsClosingPriceGray() {
        holder.bind(sampleStock(closingPricePosition = PricePosition.EQUAL))
        assertEquals(colorOf(android.R.color.darker_gray), closingPriceColor())
    }

    @Test
    fun bind_unknownPosition_colorsClosingPriceGray() {
        holder.bind(sampleStock(closingPricePosition = PricePosition.UNKNOWN))
        assertEquals(colorOf(android.R.color.darker_gray), closingPriceColor())
    }

    @Test
    fun bind_positiveDirection_colorsChangeUp() {
        holder.bind(sampleStock(changeDirection = ChangeDirection.POSITIVE))
        assertEquals(colorOf(CoreUiR.color.stock_price_up), changeColor())
    }

    @Test
    fun bind_negativeDirection_colorsChangeDown() {
        holder.bind(sampleStock(changeDirection = ChangeDirection.NEGATIVE))
        assertEquals(colorOf(CoreUiR.color.stock_price_down), changeColor())
    }

    @Test
    fun bind_zeroDirection_colorsChangeGray() {
        holder.bind(sampleStock(changeDirection = ChangeDirection.ZERO))
        assertEquals(colorOf(android.R.color.darker_gray), changeColor())
    }

    @Test
    fun bind_unknownDirection_colorsChangeGray() {
        holder.bind(sampleStock(changeDirection = ChangeDirection.UNKNOWN))
        assertEquals(colorOf(android.R.color.darker_gray), changeColor())
    }

    @Test
    fun clickingCard_invokesCallbackWithBoundStockCode() {
        holder.bind(sampleStock(code = "2317"))
        holder.itemView.performClick()
        assertEquals("2317", clickedCode)
    }
}
