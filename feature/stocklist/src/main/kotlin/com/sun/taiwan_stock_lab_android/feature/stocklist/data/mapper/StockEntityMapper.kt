package com.sun.taiwan_stock_lab_android.feature.stocklist.data.mapper

import com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity.StockEntity
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock

internal fun StockEntity.toDomain(): Stock =
    Stock(
        code = code,
        name = name,
        openingPrice = openingPrice?.toBigDecimalOrNull(),
        highestPrice = highestPrice?.toBigDecimalOrNull(),
        lowestPrice = lowestPrice?.toBigDecimalOrNull(),
        closingPrice = closingPrice?.toBigDecimalOrNull(),
        monthlyAveragePrice = monthlyAveragePrice?.toBigDecimalOrNull(),
        change = change?.toBigDecimalOrNull(),
        tradeVolume = tradeVolume,
        tradeValue = tradeValue,
        transactionCount = transactionCount,
        peRatio = peRatio?.toBigDecimalOrNull(),
        dividendYield = dividendYield?.toBigDecimalOrNull(),
        pbRatio = pbRatio?.toBigDecimalOrNull(),
    )

internal fun Stock.toEntity(): StockEntity =
    StockEntity(
        code = code,
        name = name,
        openingPrice = openingPrice?.toPlainString(),
        highestPrice = highestPrice?.toPlainString(),
        lowestPrice = lowestPrice?.toPlainString(),
        closingPrice = closingPrice?.toPlainString(),
        monthlyAveragePrice = monthlyAveragePrice?.toPlainString(),
        change = change?.toPlainString(),
        tradeVolume = tradeVolume,
        tradeValue = tradeValue,
        transactionCount = transactionCount,
        peRatio = peRatio?.toPlainString(),
        dividendYield = dividendYield?.toPlainString(),
        pbRatio = pbRatio?.toPlainString(),
    )
