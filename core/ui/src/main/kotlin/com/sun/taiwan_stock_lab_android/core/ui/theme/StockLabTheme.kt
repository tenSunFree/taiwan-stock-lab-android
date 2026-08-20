package com.sun.taiwan_stock_lab_android.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Mirrors core/ui/src/main/res/values/colors.xml so Compose and XML share
// the same stock-market color convention (rise = red, fall = green).
private val StockPriceUpLight = Color(0xFFD32F2F)
private val StockPriceDownLight = Color(0xFF2E7D32)
private val StockPriceUpDark = Color(0xFFEF5350)
private val StockPriceDownDark = Color(0xFF66BB6A)

object StockLabColors {
    val priceUp: Color
        @Composable get() = if (isSystemInDarkTheme()) StockPriceUpDark else StockPriceUpLight

    val priceDown: Color
        @Composable get() = if (isSystemInDarkTheme()) StockPriceDownDark else StockPriceDownLight
}

private val LightColorScheme = lightColorScheme()
private val DarkColorScheme = darkColorScheme()

private val StockLabTypography =
    Typography(
        labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
        titleSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    )

@Composable
fun StockLabTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = StockLabTypography,
        content = content,
    )
}
