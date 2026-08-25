package com.sun.taiwan_stock_lab_android

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sun.taiwan_stock_lab_android.core.ui.theme.StockLabTheme
import com.sun.taiwan_stock_lab_android.databinding.ActivityMainBinding
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.StockListViewModel
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.adapter.StockItemAnimator
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.adapter.StockListAdapter
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.compose.MarketSummaryBar
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEffect
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEvent
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiState
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.dialog.SortBottomSheetFragment
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.dialog.StockDetailDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: StockListViewModel by viewModels()
    private lateinit var adapter: StockListAdapter
    private val timeFormatter = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    private var lastRenderedSortDirection: SortDirection? = null
    private var latestRefreshLoadState: LoadState = LoadState.NotLoading(endOfPaginationReached = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindowInsets()
        setupToolbar()
        setupRecyclerView()
        setupComposeMarketSummary()
        setupSwipeRefresh()
        observeViewModel()
        viewModel.onEvent(StockListUiEvent.OnStart)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(top = systemBars.top)
            binding.swipeRefresh.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_sort) {
                showSortBottomSheet()
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter =
            StockListAdapter { stockCode ->
                viewModel.onEvent(StockListUiEvent.OnStockClicked(stockCode))
            }
        binding.recyclerViewStocks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
            // See StockItemAnimator: keeps ordinary add/remove/change animations, skips only the
            // large-scale move animation produced by a full stock-code sort reversal.
            itemAnimator = StockItemAnimator()
        }
    }

    private fun setupComposeMarketSummary() {
        binding.composeMarketSummary.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            StockLabTheme {
                MarketSummaryBar(summary = uiState.marketSummary)
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.onEvent(StockListUiEvent.OnRefresh)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> renderState(state) }
                }
                launch {
                    viewModel.uiEffect.collect { effect -> handleEffect(effect) }
                }
                launch {
                    // Submitting the same PagingData instance twice is a no-op internally, but
                    // collectLatest still matters here: it cancels any in-flight diff/collection
                    // against a superseded stream the moment sortDirection produces a new Pager
                    // (see StockListViewModel.stocksPagingData), instead of letting a stale
                    // collection finish first.
                    viewModel.stocksPagingData.collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }
                launch {
                    adapter.loadStateFlow.collectLatest { loadStates ->
                        latestRefreshLoadState = loadStates.refresh
                        renderContentState(viewModel.uiState.value)
                    }
                }
            }
        }
    }

    private fun renderState(state: StockListUiState) {
        val previousSortDirection = lastRenderedSortDirection
        val sortDirectionChanged = previousSortDirection != null && previousSortDirection != state.sortDirection
        lastRenderedSortDirection = state.sortDirection
        if (sortDirectionChanged) {
            // A sort-direction change re-queries Room in a new order (see
            // StockListViewModel.stocksPagingData) rather than re-sorting an in-memory list, so
            // there's no "commit callback" moment to hook the scroll reset to. Scrolling
            // immediately avoids the list appearing to jump once the new page arrives.
            binding.recyclerViewStocks.stopScroll()
            binding.recyclerViewStocks.scrollToPosition(0)
        }
        binding.swipeRefresh.isRefreshing = adapter.itemCount > 0 && state.isRefreshing
        binding.textLastUpdated.text = state.lastUpdatedAt
            ?.let { timeFormatter.format(Date(it)) }
            ?.let { getString(R.string.last_updated_format, it) } ?: getString(R.string.last_updated_unknown)
        renderContentState(state)
    }

    // Combines two independent "is loading" signals so the empty-state text can't flash between
    // them: Paging's own LoadState only reflects the *local Room query* (which finishes almost
    // instantly even when the cache is empty), while state.isRefreshing reflects the *network*
    // refresh that's populating that cache. Without combining both, a cold start on an empty
    // cache would show Room's LoadState.NotLoading + itemCount == 0 — i.e. "empty" — for the
    // second or two the network fetch is still in flight, before stocks appear.
    private fun renderContentState(state: StockListUiState) {
        val hasItems = adapter.itemCount > 0
        val isInitialLoading = !hasItems && (latestRefreshLoadState is LoadState.Loading || state.isRefreshing)
        val isEmpty = !hasItems && latestRefreshLoadState is LoadState.NotLoading && !state.isRefreshing
        binding.progressInitial.isVisible = isInitialLoading
        binding.textEmpty.isVisible = isEmpty
    }

    private fun handleEffect(effect: StockListUiEffect) {
        when (effect) {
            is StockListUiEffect.ShowStockDetail -> showStockDetail(effect)
            is StockListUiEffect.ShowError -> showError(effect.message)
        }
    }

    private fun showSortBottomSheet() {
        // showNow() commits synchronously, so this guard is reliable even against rapid
        // double-taps: show()'s commit() is queued on the main thread and could let a second
        // call slip past findFragmentByTag() before the first transaction has executed.
        if (supportFragmentManager.findFragmentByTag(SortBottomSheetFragment.TAG) != null) return
        SortBottomSheetFragment().showNow(supportFragmentManager, SortBottomSheetFragment.TAG)
    }

    private fun showStockDetail(effect: StockListUiEffect.ShowStockDetail) {
        // See showSortBottomSheet() — showNow() for the same reason.
        if (supportFragmentManager.findFragmentByTag(StockDetailDialogFragment.TAG) != null) return
        StockDetailDialogFragment
            .newInstance(effect.stock)
            .showNow(supportFragmentManager, StockDetailDialogFragment.TAG)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.recyclerViewStocks, message, Snackbar.LENGTH_LONG).show()
    }
}
