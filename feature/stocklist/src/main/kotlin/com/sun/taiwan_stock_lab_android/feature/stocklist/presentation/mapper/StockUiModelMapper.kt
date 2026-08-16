package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.ChangeDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.PricePosition
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

private const val PLACEHOLDER = "--"

fun Stock.toUiModel(): StockUiModel = StockUiModel(
    code = code,
    name = name,
    openingPrice = openingPrice.formatDecimal(),
    highestPrice = highestPrice.formatDecimal(),
    lowestPrice = lowestPrice.formatDecimal(),
    closingPrice = closingPrice.formatDecimal(),
    monthlyAveragePrice = monthlyAveragePrice.formatDecimal(),
    change = change.formatChange(),
    tradeVolume = tradeVolume.formatLong(),
    tradeValue = tradeValue.formatLong(),
    transactionCount = transactionCount.formatLong(),
    peRatio = peRatio.formatDecimal(),
    dividendYield = dividendYield.formatDecimal(),
    pbRatio = pbRatio.formatDecimal(),
    closingPricePosition = closingPricePosition(closingPrice, monthlyAveragePrice),
    changeDirection = changeDirection(change),
)

private fun BigDecimal?.formatDecimal(): String {
    if (this == null) return PLACEHOLDER
    return DecimalFormat("#,##0.##").format(this)
}

private fun BigDecimal?.formatChange(): String {
    if (this == null) return PLACEHOLDER
    val formatted = DecimalFormat("#,##0.##").format(this.abs())
    return when {
        this > BigDecimal.ZERO -> "+$formatted"
        this < BigDecimal.ZERO -> "-$formatted"
        else -> "0"
    }
}

private fun Long?.formatLong(): String =
    this?.let { NumberFormat.getIntegerInstance(Locale.TAIWAN).format(it) } ?: PLACEHOLDER

private fun closingPricePosition(closing: BigDecimal?, average: BigDecimal?): PricePosition {
    if (closing == null || average == null) return PricePosition.UNKNOWN
    return when {
        closing > average -> PricePosition.ABOVE_AVERAGE
        closing < average -> PricePosition.BELOW_AVERAGE
        else -> PricePosition.EQUAL
    }
}

private fun changeDirection(change: BigDecimal?): ChangeDirection {
    if (change == null) return ChangeDirection.UNKNOWN
    return when {
        change > BigDecimal.ZERO -> ChangeDirection.POSITIVE
        change < BigDecimal.ZERO -> ChangeDirection.NEGATIVE
        else -> ChangeDirection.ZERO
    }
}