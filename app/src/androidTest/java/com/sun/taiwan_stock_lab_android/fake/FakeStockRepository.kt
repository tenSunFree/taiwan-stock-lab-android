package com.sun.taiwan_stock_lab_android.fake

import androidx.paging.PagingData
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.MarketChangeSummary
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.Stock
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class FakeStockRepository : StockRepository {
    private val stocks = MutableStateFlow(sampleStocks())
    private val lastRefreshedAt = MutableStateFlow<Long?>(null)

    override fun observeStocksPaged(direction: SortDirection): Flow<PagingData<Stock>> =
        stocks.map { list ->
            val ordered =
                if (direction ==
                    SortDirection.ASCENDING
                ) {
                    list.sortedBy { it.code }
                } else {
                    list.sortedByDescending { it.code }
                }
            PagingData.from(ordered)
        }

    override suspend fun getStock(code: String): Stock? = stocks.value.find { it.code == code }

    // It should be calculated from the current stock list, rather than using a fixed constant, to avoid decoupling from the stock list content.
    override fun observeMarketSummary(): Flow<MarketChangeSummary> =
        stocks.map { list ->
            MarketChangeSummary(
                advancingCount = list.count { (it.change ?: BigDecimal.ZERO) > BigDecimal.ZERO },
                decliningCount = list.count { (it.change ?: BigDecimal.ZERO) < BigDecimal.ZERO },
                unchangedCount = list.count { (it.change ?: BigDecimal.ZERO).compareTo(BigDecimal.ZERO) == 0 },
            )
        }

    override fun observeLastRefreshedAt(): Flow<Long?> = lastRefreshedAt.asStateFlow()

    override suspend fun refreshStocks(): Result<Unit> {
        lastRefreshedAt.value = System.currentTimeMillis()
        return Result.success(Unit)
    }

    // 2330 change = +25 (increase), 2317 change = -1 (decrease)
    // → observeMarketSummary() calculates advancing=1 / declining=1 / unchanged=0, which is consistent with the list content.
    private fun sampleStocks() =
        listOf(
            Stock(
                code = "2330",
                name = "台積電",
                openingPrice = BigDecimal("1480"),
                highestPrice = BigDecimal("1500"),
                lowestPrice = BigDecimal("1465"),
                closingPrice = BigDecimal("1485"),
                monthlyAveragePrice = BigDecimal("1420"),
                change = BigDecimal("25"),
                tradeVolume = 27_300_000,
                tradeValue = 40_200_000_000,
                transactionCount = 35_842,
                peRatio = BigDecimal("18.5"),
                dividendYield = BigDecimal("2.1"),
                pbRatio = BigDecimal("5.3"),
            ),
            Stock(
                code = "2317",
                name = "鴻海",
                openingPrice = BigDecimal("100"),
                highestPrice = BigDecimal("102"),
                lowestPrice = BigDecimal("99"),
                closingPrice = BigDecimal("101"),
                monthlyAveragePrice = BigDecimal("98"),
                change = BigDecimal("-1"),
                tradeVolume = 10_000_000,
                tradeValue = 1_000_000_000,
                transactionCount = 5_000,
                peRatio = BigDecimal("10.2"),
                dividendYield = BigDecimal("4.5"),
                pbRatio = BigDecimal("1.8"),
            ),
        )
}
