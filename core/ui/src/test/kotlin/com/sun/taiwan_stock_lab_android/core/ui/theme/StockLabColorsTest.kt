package com.sun.taiwan_stock_lab_android.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * StockLabTheme.kt's Compose color constants are documented as mirroring
 * core/ui/src/main/res/{values,values-night}/colors.xml. Nothing else enforces that — this test
 * catches drift if one side is edited without the other.
 */
class StockLabColorsTest {
    @Test
    fun lightColorConstants_matchValuesColorsXml() {
        val resourceColors = parseColorsXml("src/main/res/values/colors.xml")
        assertEquals(resourceColors.getValue("stock_price_up"), StockPriceUpLight.toHex())
        assertEquals(resourceColors.getValue("stock_price_down"), StockPriceDownLight.toHex())
    }

    @Test
    fun darkColorConstants_matchValuesNightColorsXml() {
        val resourceColors = parseColorsXml("src/main/res/values-night/colors.xml")
        assertEquals(resourceColors.getValue("stock_price_up"), StockPriceUpDark.toHex())
        assertEquals(resourceColors.getValue("stock_price_down"), StockPriceDownDark.toHex())
    }

    private fun Color.toHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

    private fun parseColorsXml(relativePath: String): Map<String, String> {
        val document =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(relativePath))
        val colorNodes = document.getElementsByTagName("color")
        return buildMap {
            for (index in 0 until colorNodes.length) {
                val node = colorNodes.item(index)
                val name = node.attributes.getNamedItem("name").nodeValue
                put(name, node.textContent.trim())
            }
        }
    }
}
