package com.sun.taiwan_stock_lab_android.feature.stocklist.data.repository

internal class EmptyStockSnapshotException :
    IllegalStateException("TWSE refresh returned no valid stocks.")