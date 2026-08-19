package com.sun.taiwan_stock_lab_android.feature.stocklist.data.mapper

import java.math.BigDecimal

internal object TwseNumericParser {
    private val noDataValues = setOf("", "-", "--", "---")

    fun parseDecimal(raw: String?): BigDecimal? = normalize(raw)?.toBigDecimalOrNull()

    fun parseLong(raw: String?): Long? = normalize(raw)?.toLongOrNull()

    private fun normalize(raw: String?): String? =
        raw
            ?.trim()
            ?.takeUnless { it in noDataValues }
            ?.replace(",", "")
            ?.takeIf { it.isNotEmpty() }
}
