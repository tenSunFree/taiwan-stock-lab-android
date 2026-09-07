package com.sun.taiwan_stock_lab_android.core.common.coroutine

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertSame
import org.junit.Test

class DefaultDispatchersProviderTest {
    private val provider: DispatchersProvider = DefaultDispatchersProvider()

    @Test
    fun `main returns Dispatchers Main`() {
        assertSame(Dispatchers.Main, provider.main)
    }

    @Test
    fun `io returns Dispatchers IO`() {
        assertSame(Dispatchers.IO, provider.io)
    }

    @Test
    fun `default returns Dispatchers Default`() {
        assertSame(Dispatchers.Default, provider.default)
    }
}
