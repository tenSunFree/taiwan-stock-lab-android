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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sun.taiwan_stock_lab_android.core.ui.theme.StockLabTheme
import com.sun.taiwan_stock_lab_android.databinding.ActivityMainBinding
import com.sun.taiwan_stock_lab_android.databinding.BottomSheetSortBinding
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.StockListViewModel
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.adapter.StockListAdapter
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.compose.MarketSummaryBar
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEffect
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEvent
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiState
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.mapper.computeMarketSummary
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel
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
            // Diagnostic: disabling the default ItemAnimator to determine
            // whether large-scale DiffUtil move animations (from a full
            // stock-code sort reversal across ~1000+ rows) were responsible
            // for the "flies to the bottom" visual artifact on sort change.
            itemAnimator = null
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
            is StockListUiEffect.ShowStockDetail -> showStockDetailDialog(effect.stock)
            is StockListUiEffect.ShowError -> showError(effect.message)
        }
    }

    private fun showSortBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetSortBinding.inflate(layoutInflater)
        val currentDirection = viewModel.uiState.value.sortDirection
        val checkedId =
            when (currentDirection) {
                SortDirection.DESCENDING -> R.id.radioSortDescending
                SortDirection.ASCENDING -> R.id.radioSortAscending
            }
        sheetBinding.radioGroupSort.check(checkedId)
        sheetBinding.radioGroupSort.setOnCheckedChangeListener { _, checkedId ->
            val selectedDirection =
                when (checkedId) {
                    R.id.radioSortDescending -> SortDirection.DESCENDING
                    R.id.radioSortAscending -> SortDirection.ASCENDING
                    else -> return@setOnCheckedChangeListener
                }
            dialog.dismiss()
            if (selectedDirection == currentDirection) return@setOnCheckedChangeListener
            binding.recyclerViewStocks.stopScroll()
            viewModel.onEvent(StockListUiEvent.OnSortDirectionSelected(selectedDirection))
        }
        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    private fun showStockDetailDialog(stock: StockUiModel) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.stock_detail_title, stock.name, stock.code))
            .setMessage(
                listOf(
                    getString(R.string.stock_detail_pe_ratio, stock.peRatio),
                    getString(R.string.stock_detail_dividend_yield, stock.dividendYield),
                    getString(R.string.stock_detail_pb_ratio, stock.pbRatio),
                ).joinToString("\n"),
            ).setPositiveButton(R.string.dialog_confirm, null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.recyclerViewStocks, message, Snackbar.LENGTH_LONG).show()
    }
}
