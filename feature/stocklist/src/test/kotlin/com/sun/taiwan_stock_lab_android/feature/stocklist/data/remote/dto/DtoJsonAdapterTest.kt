package com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The @JsonClass(generateAdapter = true) annotation on each DTO makes KSP generate a
 * *JsonAdapter class per DTO at compile time. That generated adapter is real production code
 * (it's what actually turns the TWSE HTTP response into these DTOs) but nothing exercises it:
 * every other test builds DTOs directly via their constructor. Driving them through a real
 * Moshi instance here is a pure-JVM way to cover that generated parsing logic.
 */
class DtoJsonAdapterTest {
    private val moshi = Moshi.Builder().build()

    @Test
    fun `StockDayDto adapter maps every TWSE field by its JSON name`() {
        val adapter = moshi.adapter(StockDayDto::class.java)
        val json =
            """
            {
              "Code": "2330",
              "Name": "台積電",
              "TradeVolume": "27,300,000",
              "TradeValue": "39,780,000,000",
              "OpeningPrice": "1,455.00",
              "HighestPrice": "1,460.00",
              "LowestPrice": "1,450.00",
              "ClosingPrice": "1,455.00",
              "Change": "25.00",
              "Transaction": "18,234"
            }
            """.trimIndent()
        val dto = adapter.fromJson(json)
        assertEquals("2330", dto?.code)
        assertEquals("台積電", dto?.name)
        assertEquals("27,300,000", dto?.tradeVolume)
        assertEquals("39,780,000,000", dto?.tradeValue)
        assertEquals("1,455.00", dto?.openingPrice)
        assertEquals("1,460.00", dto?.highestPrice)
        assertEquals("1,450.00", dto?.lowestPrice)
        assertEquals("1,455.00", dto?.closingPrice)
        assertEquals("25.00", dto?.change)
        assertEquals("18,234", dto?.transaction)
    }

    @Test
    fun `StockDayDto adapter defaults missing fields to null`() {
        val adapter = moshi.adapter(StockDayDto::class.java)
        val dto = adapter.fromJson("""{"Code": "2330"}""")
        assertEquals("2330", dto?.code)
        assertNull(dto?.name)
        assertNull(dto?.tradeVolume)
    }

    @Test
    fun `StockDayDto list adapter parses a TWSE-shaped JSON array`() {
        val type = Types.newParameterizedType(List::class.java, StockDayDto::class.java)
        val adapter = moshi.adapter<List<StockDayDto>>(type)
        val json = """[{"Code": "2330", "Name": "台積電"}, {"Code": "0050", "Name": "元大台灣50"}]"""
        val dtos = adapter.fromJson(json)
        assertEquals(2, dtos?.size)
        assertEquals("2330", dtos?.get(0)?.code)
        assertEquals("0050", dtos?.get(1)?.code)
    }

    @Test
    fun `StockDayAverageDto adapter maps every TWSE field by its JSON name`() {
        val adapter = moshi.adapter(StockDayAverageDto::class.java)
        val json =
            """
            {
              "Code": "2330",
              "Name": "台積電",
              "ClosingPrice": "1,455.00",
              "MonthlyAveragePrice": "1,420.50"
            }
            """.trimIndent()
        val dto = adapter.fromJson(json)
        assertEquals("2330", dto?.code)
        assertEquals("台積電", dto?.name)
        assertEquals("1,455.00", dto?.closingPrice)
        assertEquals("1,420.50", dto?.monthlyAveragePrice)
    }

    @Test
    fun `StockValuationDto adapter maps every TWSE field by its JSON name`() {
        val adapter = moshi.adapter(StockValuationDto::class.java)
        val json =
            """
            {
              "Code": "2330",
              "Name": "台積電",
              "PEratio": "24.31",
              "DividendYield": "1.82",
              "PBratio": "6.72"
            }
            """.trimIndent()
        val dto = adapter.fromJson(json)
        assertEquals("2330", dto?.code)
        assertEquals("台積電", dto?.name)
        assertEquals("24.31", dto?.peRatio)
        assertEquals("1.82", dto?.dividendYield)
        assertEquals("6.72", dto?.pbRatio)
    }

    @Test
    fun `StockValuationDto adapter defaults missing fields to null`() {
        val adapter = moshi.adapter(StockValuationDto::class.java)
        val dto = adapter.fromJson("""{"Code": "2330", "Name": "台積電"}""")
        assertNull(dto?.peRatio)
        assertNull(dto?.dividendYield)
        assertNull(dto?.pbRatio)
    }
}
