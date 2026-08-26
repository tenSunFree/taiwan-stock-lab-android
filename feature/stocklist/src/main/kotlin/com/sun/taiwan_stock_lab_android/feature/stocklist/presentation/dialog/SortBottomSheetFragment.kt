package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sun.taiwan_stock_lab_android.feature.stocklist.R
import com.sun.taiwan_stock_lab_android.feature.stocklist.databinding.BottomSheetSortBinding
import com.sun.taiwan_stock_lab_android.feature.stocklist.domain.model.SortDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.StockListViewModel
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

    // A click listener on each radio button, rather than RadioGroup.OnCheckedChangeListener.
    // The latter only fires when the checked state actually changes, so tapping the option that
    // is already selected would silently do nothing and leave the sheet open with no feedback.
    private val onOptionClicked =
        View.OnClickListener { view ->
            val selectedDirection =
                when (view.id) {
                    R.id.radioSortDescending -> SortDirection.DESCENDING
                    R.id.radioSortAscending -> SortDirection.ASCENDING
                    else -> return@OnClickListener
                }
            // StockListViewModel.onSortDirectionSelected() is already a no-op when the direction
            // hasn't changed, so re-selecting the current direction just dismisses the sheet
            // without triggering an unnecessary re-sort.
            viewModel.onEvent(StockListUiEvent.OnSortDirectionSelected(selectedDirection))
            dismiss()
        }

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
        binding.radioSortDescending.setOnClickListener(onOptionClicked)
        binding.radioSortAscending.setOnClickListener(onOptionClicked)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
