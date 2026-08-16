package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockDatabaseMigrationTest {

    private val dbFile =
        InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath("migration-test.db")

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = dbFile,
        databaseClass = StockDatabase::class,
        driver = BundledSQLiteDriver(),
    )

    @Test
    fun migrate1To2_preservesExistingStocksAndAddsMetadataTable() = runTest {
        migrationTestHelper.createDatabase(1).let { connection ->
            try {
                connection.execSQL(
                    """
                    INSERT INTO stocks (code, name, openingPrice, highestPrice, lowestPrice,
                        closingPrice, monthlyAveragePrice, change, tradeVolume, tradeValue,
                        transactionCount, peRatio, dividendYield, pbRatio)
                    VALUES ('2330', '台積電', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
                    """.trimIndent(),
                )
            } finally {
                connection.close()
            }
        }

        migrationTestHelper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).let { connection ->
            try {
                connection.prepare("SELECT code FROM stocks").let { statement ->
                    try {
                        assertTrue(statement.step())
                        assertEquals("2330", statement.getText(0))
                    } finally {
                        statement.close()
                    }
                }
                connection.prepare("SELECT COUNT(*) FROM refresh_metadata").let { statement ->
                    try {
                        assertTrue(statement.step())
                        assertEquals(0L, statement.getLong(0))
                    } finally {
                        statement.close()
                    }
                }
            } finally {
                connection.close()
            }
        }
    }
}
