package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sun.taiwan_stock_lab_android.core.ui.theme.StockLabColors
import com.sun.taiwan_stock_lab_android.core.ui.theme.StockLabTheme
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.MarketSummary

@Composable
fun MarketSummaryBar(
    summary: MarketSummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SummaryItem(label = "上漲", count = summary.advancingCount, color = StockLabColors.priceUp)
        SummaryItem(
            label = "下跌",
            count = summary.decliningCount,
            color = StockLabColors.priceDown,
        )
        SummaryItem(
            label = "平盤",
            count = summary.unchangedCount,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryItem(
    label: String,
    count: Int,
    color: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = "$count", style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Preview(showBackground = true)
@Composable
private fun MarketSummaryBarPreview() {
    StockLabTheme {
        MarketSummaryBar(
            summary =
                MarketSummary(
                    advancingCount = 812,
                    decliningCount = 431,
                    unchangedCount = 57,
                ),
        )
    }
}
