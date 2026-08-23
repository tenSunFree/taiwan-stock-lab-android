package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sun.taiwan_stock_lab_android.feature.stocklist.R
import com.sun.taiwan_stock_lab_android.feature.stocklist.databinding.BottomSheetSortBinding
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.StockListViewModel
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.contract.StockListUiEvent

/**
 * Shown via [androidx.fragment.app.FragmentManager] rather than as a plain
 * [com.google.android.material.bottomsheet.BottomSheetDialog], so it survives configuration
 * changes (e.g. rotation) instead of being dismissed when the host Activity is recreated.
 *
 * Sort direction stays screen state owned by the shared, activity-scoped [StockListViewModel] —
 * this Fragment reads it fresh every time its view is created rather than storing a separate copy
 * in Fragment arguments, so a restored instance after rotation always reflects the latest state.
 */
class SortBottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "SortBottomSheetFragment"
    }

    private val viewModel: StockListViewModel by activityViewModels()

    // Not exposed outside this class, so a plain nullable var is used instead of the
    // underscore-prefixed backing-property convention (which ktlint only permits when the
    // matching accessor is public).
    private var binding: BottomSheetSortBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = BottomSheetSortBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val binding = checkNotNull(binding)
        val currentDirection = viewModel.uiState.value.sortDirection
        binding.radioGroupSort.check(
            when (currentDirection) {
                SortDirection.DESCENDING -> R.id.radioSortDescending
                SortDirection.ASCENDING -> R.id.radioSortAscending
            },
        )
        binding.radioGroupSort.setOnCheckedChangeListener { _, checkedId ->
            val selectedDirection =
                when (checkedId) {
                    R.id.radioSortDescending -> SortDirection.DESCENDING
                    R.id.radioSortAscending -> SortDirection.ASCENDING
                    else -> return@setOnCheckedChangeListener
                }
            viewModel.onEvent(StockListUiEvent.OnSortDirectionSelected(selectedDirection))
            dismiss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
