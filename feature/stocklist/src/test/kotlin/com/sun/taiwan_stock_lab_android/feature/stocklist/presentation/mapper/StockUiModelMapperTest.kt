package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.ChangeDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.PricePosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class StockUiModelMapperTest {
    private val base =
        Stock(
            code = "2330",
            name = "台積電",
            openingPrice = null,
            highestPrice = null,
            lowestPrice = null,
            closingPrice = null,
            monthlyAveragePrice = null,
            change = null,
            tradeVolume = null,
            tradeValue = null,
            transactionCount = null,
            peRatio = null,
            dividendYield = null,
            pbRatio = null,
        )

    @Test
    fun `closing above monthly average is marked above average`() {
        val stock =
            base.copy(
                closingPrice = BigDecimal("1485.00"),
                monthlyAveragePrice = BigDecimal("1420.50"),
            )
        assertEquals(PricePosition.ABOVE_AVERAGE, stock.toUiModel().closingPricePosition)
    }

    @Test
    fun `closing below monthly average is marked below average`() {
        val stock =
            base.copy(
                closingPrice = BigDecimal("1400.00"),
                monthlyAveragePrice = BigDecimal("1420.50"),
            )
        assertEquals(PricePosition.BELOW_AVERAGE, stock.toUiModel().closingPricePosition)
    }

    @Test
    fun `positive change is marked positive`() {
        val stock = base.copy(change = BigDecimal("25.00"))
        assertEquals(ChangeDirection.POSITIVE, stock.toUiModel().changeDirection)
    }

    @Test
    fun `negative change is marked negative`() {
        val stock = base.copy(change = BigDecimal("-10.00"))
        assertEquals(ChangeDirection.NEGATIVE, stock.toUiModel().changeDirection)
    }

    @Test
    fun `null values use placeholder`() {
        val model = base.toUiModel()
        assertEquals("--", model.closingPrice)
        assertEquals("--", model.peRatio)
        assertEquals(PricePosition.UNKNOWN, model.closingPricePosition)
        assertEquals(ChangeDirection.UNKNOWN, model.changeDirection)
    }

    @Test
    fun `large numeric values use thousands separators`() {
        val stock = base.copy(tradeVolume = 27_300_000L)
        assertEquals("27,300,000", stock.toUiModel().tradeVolume)
    }

    @Test
    fun `positive change includes plus sign`() {
        val stock = base.copy(change = BigDecimal("25.00"))
        assertEquals("+25", stock.toUiModel().change)
    }
}
