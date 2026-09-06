package com.sun.taiwan_stock_lab_android.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TwseNetworkModuleTest {
    @Test
    fun provideTwseRetrofit_pointsAtTheTwseOpenApiBaseUrl() {
        val retrofit = TwseNetworkModule.provideTwseRetrofit()
        assertEquals("https://openapi.twse.com.tw/", retrofit.baseUrl().toString())
    }

    @Test
    fun provideTwseApiService_buildsAWorkingServiceProxy() {
        val retrofit = TwseNetworkModule.provideTwseRetrofit()
        val apiService = TwseNetworkModule.provideTwseApiService(retrofit)
        assertNotNull(apiService)
    }
}
