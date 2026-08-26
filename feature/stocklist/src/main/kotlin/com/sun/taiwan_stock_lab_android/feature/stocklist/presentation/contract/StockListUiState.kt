package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract

import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.MarketSummary

// `stocks` and `hasLoadedCache` were removed: the RecyclerView's list content is now a separate
// Flow<PagingData<StockUiModel>> (StockListViewModel.stocksPagingData) rather than part of this
// state — PagingData isn't meaningfully comparable/storable in a plain data class, and is
// designed to be collected by exactly one PagingDataAdapter. The "cache not loaded yet" vs.
// "loaded but empty" distinction that hasLoadedCache used to encode is now covered natively by
// the adapter's loadStateFlow (LoadState.Loading vs. LoadState.NotLoading + itemCount == 0).
data class StockListUiState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val lastUpdatedAt: Long? = null,
    val marketSummary: MarketSummary = MarketSummary(advancingCount = 0, decliningCount = 0, unchangedCount = 0),
)
