package com.sun.taiwan_stock_lab_android.feature.stocklist.data.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayAverageDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockValuationDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.model.TwseRawSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class StockMapperTest {
    @Test
    fun `merge joins three datasets by code`() {
        val snapshot =
            TwseRawSnapshot(
                valuations =
                    listOf(
                        StockValuationDto(
                            code = "2330",
                            peRatio = "24.31",
                            dividendYield = "1.82",
                            pbRatio = "6.72",
                        ),
                    ),
                dayAverages =
                    listOf(
                        StockDayAverageDto(code = "2330", monthlyAveragePrice = "1420.50"),
                    ),
                days =
                    listOf(
                        StockDayDto(
                            code = "2330",
                            name = "台積電",
                            tradeVolume = "27,300,000",
                            change = "25.00",
                        ),
                    ),
            )
        val stock = StockMapper.merge(snapshot).single()
        assertEquals("2330", stock.code)
        assertEquals("台積電", stock.name)
        assertEquals(BigDecimal("24.31"), stock.peRatio)
        assertEquals(BigDecimal("1420.50"), stock.monthlyAveragePrice)
        assertEquals(27_300_000L, stock.tradeVolume)
    }

    @Test
    fun `merge keeps stock when valuation data is missing`() {
        val snapshot =
            TwseRawSnapshot(
                valuations = emptyList(),
                dayAverages = emptyList(),
                days = listOf(StockDayDto(code = "0050", name = "元大台灣50")),
            )
        val stock = StockMapper.merge(snapshot).single()
        assertEquals("0050", stock.code)
        assertNull(stock.peRatio)
        assertNull(stock.dividendYield)
        assertNull(stock.pbRatio)
        assertNull(stock.monthlyAveragePrice)
    }

    @Test
    fun `merge skips stock when code is missing`() {
        val snapshot =
            TwseRawSnapshot(
                valuations = emptyList(),
                dayAverages = emptyList(),
                days = listOf(StockDayDto(code = null, name = "Invalid")),
            )
        assertTrue(StockMapper.merge(snapshot).isEmpty())
    }

    @Test
    fun `merge skips stock when name is missing`() {
        val snapshot =
            TwseRawSnapshot(
                valuations = emptyList(),
                dayAverages = emptyList(),
                days = listOf(StockDayDto(code = "2330", name = null)),
            )
        assertTrue(StockMapper.merge(snapshot).isEmpty())
    }
}
