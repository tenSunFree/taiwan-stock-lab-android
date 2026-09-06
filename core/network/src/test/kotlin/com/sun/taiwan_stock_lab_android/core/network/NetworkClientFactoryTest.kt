package com.sun.taiwan_stock_lab_android.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkClientFactoryTest {
    @Test
    fun createRetrofit_usesTheProvidedBaseUrl() {
        val retrofit = NetworkClientFactory.createRetrofit(baseUrl = "https://example.com/")
        assertEquals("https://example.com/", retrofit.baseUrl().toString())
    }

    @Test
    fun createRetrofit_registersAMoshiConverterFactory() {
        val retrofit = NetworkClientFactory.createRetrofit(baseUrl = "https://example.com/")
        assertTrue(retrofit.converterFactories().any { it.toString().contains("Moshi") })
    }
}
