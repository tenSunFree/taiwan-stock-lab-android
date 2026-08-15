package com.sun.taiwan_stock_lab_android.feature.stocklist.data.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayAverageDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockDayDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.dto.StockValuationDto
import com.sun.taiwan_stock_lab_android.feature.stocklist.data.remote.model.TwseRawSnapshot
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock

/**
 * Merges the three TWSE datasets by stock code, using STOCK_DAY_ALL as the stock universe.
 * A stock missing from BWIBBU_ALL or STOCK_DAY_AVG_ALL still appears, with the
 * corresponding fields set to null.
 */
object StockMapper {

    fun merge(snapshot: TwseRawSnapshot): List<Stock> {
        val valuationByCode = snapshot.valuations.associateByValidCode { it.code }
        val averageByCode = snapshot.dayAverages.associateByValidCode { it.code }
        return snapshot.days.mapNotNull { day ->
            val code = day.code.normalizedRequiredValue() ?: return@mapNotNull null
            val name = day.name.normalizedRequiredValue() ?: return@mapNotNull null
            toStock(code, name, day, valuationByCode[code], averageByCode[code])
        }
    }

    private fun toStock(
        code: String,
        name: String,
        day: StockDayDto,
        valuation: StockValuationDto?,
        average: StockDayAverageDto?,
    ): Stock = Stock(
        code = code,
        name = name,
        openingPrice = TwseNumericParser.parseDecimal(day.openingPrice),
        highestPrice = TwseNumericParser.parseDecimal(day.highestPrice),
        lowestPrice = TwseNumericParser.parseDecimal(day.lowestPrice),
        closingPrice = TwseNumericParser.parseDecimal(day.closingPrice),
        monthlyAveragePrice = TwseNumericParser.parseDecimal(average?.monthlyAveragePrice),
        change = TwseNumericParser.parseDecimal(day.change),
        tradeVolume = TwseNumericParser.parseLong(day.tradeVolume),
        tradeValue = TwseNumericParser.parseLong(day.tradeValue),
        transactionCount = TwseNumericParser.parseLong(day.transaction),
        peRatio = TwseNumericParser.parseDecimal(valuation?.peRatio),
        dividendYield = TwseNumericParser.parseDecimal(valuation?.dividendYield),
        pbRatio = TwseNumericParser.parseDecimal(valuation?.pbRatio),
    )

    private fun <T> List<T>.associateByValidCode(
        getCode: (T) -> String?
    ): Map<String, T> = mapNotNull { item ->
        getCode(item).normalizedRequiredValue()?.let { it to item }
    }.toMap()

    private fun String?.normalizedRequiredValue(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
