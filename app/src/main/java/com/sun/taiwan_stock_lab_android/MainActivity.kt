package com.sun.taiwan_stock_lab_android

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sun.taiwan_stock_lab_android.core.ui.theme.StockLabTheme
import com.sun.taiwan_stock_lab_android.databinding.ActivityMainBinding
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.StockListViewModel
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.adapter.StockItemAnimator
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.adapter.StockListAdapter
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.compose.MarketSummaryBar
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEffect
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEvent
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiState
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.dialog.SortBottomSheetFragment
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.dialog.StockDetailDialogFragment
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper.computeMarketSummary
import dagger.hilt.android.AndroidEntryPoint
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
            val summary = remember(uiState.stocks) { computeMarketSummary(uiState.stocks) }
            StockLabTheme {
                MarketSummaryBar(summary = summary)
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
            }
        }
    }

    private fun renderState(state: StockListUiState) {
        val previousSortDirection = lastRenderedSortDirection
        val sortDirectionChanged =
            previousSortDirection != null && previousSortDirection != state.sortDirection
        lastRenderedSortDirection = state.sortDirection
        adapter.submitList(state.stocks) {
            if (sortDirectionChanged) {
                binding.recyclerViewStocks.stopScroll()
                binding.recyclerViewStocks.scrollToPosition(0)
            }
        }
        binding.swipeRefresh.isRefreshing =
            state.hasLoadedCache &&
            state.stocks.isNotEmpty() &&
            state.isRefreshing
        val isInitialLoading =
            !state.hasLoadedCache || (state.stocks.isEmpty() && state.isRefreshing)
        binding.progressInitial.isVisible = isInitialLoading
        binding.textEmpty.isVisible =
            state.hasLoadedCache &&
            state.stocks.isEmpty() &&
            !state.isRefreshing
        binding.textLastUpdated.text = state.lastUpdatedAt
            ?.let { timeFormatter.format(Date(it)) }
            ?.let { getString(R.string.last_updated_format, it) }
            ?: getString(R.string.last_updated_unknown)
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
