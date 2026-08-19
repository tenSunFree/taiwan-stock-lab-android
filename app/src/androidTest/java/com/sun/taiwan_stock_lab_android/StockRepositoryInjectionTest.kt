package com.sun.taiwan_stock_lab_android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.repository.StockRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StockRepositoryInjectionTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var stockRepository: StockRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun stockRepository_isInjectedSuccessfully() {
        assertNotNull(stockRepository)
    }
}
