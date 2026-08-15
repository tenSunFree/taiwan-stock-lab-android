package com.sun.taiwan_stock_lab_android.feature.stocklist.data.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

class TwseNumericParserTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" ", "-", "--", "---", "invalid"])
    fun `parseDecimal returns null for missing or invalid values`(input: String?) {
        assertNull(TwseNumericParser.parseDecimal(input))
    }

    @Test
    fun `parseDecimal parses decimal value`() {
        assertEquals(BigDecimal("1485.00"), TwseNumericParser.parseDecimal("1485.00"))
    }

    @Test
    fun `parseDecimal removes thousands separators`() {
        assertEquals(BigDecimal("1234567.89"), TwseNumericParser.parseDecimal("1,234,567.89"))
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" ", "-", "--", "---", "invalid"])
    fun `parseLong returns null for missing or invalid values`(input: String?) {
        assertNull(TwseNumericParser.parseLong(input))
    }

    @Test
    fun `parseLong removes thousands separators`() {
        assertEquals(1_234_567L, TwseNumericParser.parseLong("1,234,567"))
    }
}