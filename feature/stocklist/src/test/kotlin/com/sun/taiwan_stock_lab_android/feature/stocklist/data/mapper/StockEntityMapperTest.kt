package com.sun.taiwan_stock_lab_android.feature.stocklist.data.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class StockEntityMapperTest {
    @Test
    fun `toDomain maps every field from entity to domain model`() {
        val stock = fullEntity().toDomain()
        assertEquals("2330", stock.code)
        assertEquals("台積電", stock.name)
        assertEquals(BigDecimal("580.0"), stock.openingPrice)
        assertEquals(BigDecimal("590.0"), stock.highestPrice)
        assertEquals(BigDecimal("575.0"), stock.lowestPrice)
        assertEquals(BigDecimal("585.0"), stock.closingPrice)
        assertEquals(BigDecimal("583.5"), stock.monthlyAveragePrice)
        assertEquals(BigDecimal("5.0"), stock.change)
        assertEquals(35_000_000L, stock.tradeVolume)
        assertEquals(20_000_000_000L, stock.tradeValue)
        assertEquals(15_000L, stock.transactionCount)
        assertEquals(BigDecimal("18.2"), stock.peRatio)
        assertEquals(BigDecimal("2.1"), stock.dividendYield)
        assertEquals(BigDecimal("6.5"), stock.pbRatio)
    }

    @Test
    fun `toDomain converts invalid or empty decimal strings to null`() {
        val stock =
            fullEntity()
                .copy(
                    openingPrice = "invalid",
                    highestPrice = "-",
                    lowestPrice = "",
                    peRatio = "abc",
                ).toDomain()
        assertNull(stock.openingPrice)
        assertNull(stock.highestPrice)
        assertNull(stock.lowestPrice)
        assertNull(stock.peRatio)
    }

    @Test
    fun `toDomain keeps null numeric fields as null`() {
        val stock =
            fullEntity()
                .copy(
                    openingPrice = null,
                    closingPrice = null,
                    change = null,
                    dividendYield = null,
                ).toDomain()
        assertNull(stock.openingPrice)
        assertNull(stock.closingPrice)
        assertNull(stock.change)
        assertNull(stock.dividendYield)
    }

    @Test
    fun `toEntity maps every field from domain model to entity`() {
        val entity = fullStock().toEntity()
        assertEquals("2330", entity.code)
        assertEquals("台積電", entity.name)
        assertEquals("580", entity.openingPrice)
        assertEquals("590", entity.highestPrice)
        assertEquals("575", entity.lowestPrice)
        assertEquals("585", entity.closingPrice)
        assertEquals("583.5", entity.monthlyAveragePrice)
        assertEquals("5", entity.change)
        assertEquals(35_000_000L, entity.tradeVolume)
        assertEquals(20_000_000_000L, entity.tradeValue)
        assertEquals(15_000L, entity.transactionCount)
        assertEquals("18.2", entity.peRatio)
        assertEquals("2.1", entity.dividendYield)
        assertEquals("6.5", entity.pbRatio)
    }

    @Test
    fun `toEntity uses plain string representation for BigDecimal`() {
        val entity = fullStock().copy(openingPrice = BigDecimal("1E+3")).toEntity()
        assertEquals("1000", entity.openingPrice)
    }

    @Test
    fun `toEntity keeps null numeric fields as null`() {
        val entity =
            fullStock()
                .copy(
                    openingPrice = null,
                    closingPrice = null,
                    change = null,
                    peRatio = null,
                ).toEntity()
        assertNull(entity.openingPrice)
        assertNull(entity.closingPrice)
        assertNull(entity.change)
        assertNull(entity.peRatio)
    }

    private fun fullEntity() =
        StockEntity(
            code = "2330",
            name = "台積電",
            openingPrice = "580.0",
            highestPrice = "590.0",
            lowestPrice = "575.0",
            closingPrice = "585.0",
            monthlyAveragePrice = "583.5",
            change = "5.0",
            tradeVolume = 35_000_000L,
            tradeValue = 20_000_000_000L,
            transactionCount = 15_000L,
            peRatio = "18.2",
            dividendYield = "2.1",
            pbRatio = "6.5",
        )

    private fun fullStock() =
        Stock(
            code = "2330",
            name = "台積電",
            openingPrice = BigDecimal("580"),
            highestPrice = BigDecimal("590"),
            lowestPrice = BigDecimal("575"),
            closingPrice = BigDecimal("585"),
            monthlyAveragePrice = BigDecimal("583.5"),
            change = BigDecimal("5"),
            tradeVolume = 35_000_000L,
            tradeValue = 20_000_000_000L,
            transactionCount = 15_000L,
            peRatio = BigDecimal("18.2"),
            dividendYield = BigDecimal("2.1"),
            pbRatio = BigDecimal("6.5"),
        )
}
