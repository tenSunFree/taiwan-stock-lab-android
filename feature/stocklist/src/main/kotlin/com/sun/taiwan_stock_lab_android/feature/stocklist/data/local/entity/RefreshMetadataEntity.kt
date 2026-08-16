package com.sun.taiwan_stock_lab_android.feature.stocklist.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "refresh_metadata")
data class RefreshMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastSuccessfulRefreshAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}