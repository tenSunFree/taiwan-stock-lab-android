package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sun.taiwan_stock_lab_android.feature.stocklist.R
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel

/**
 * Displays a snapshot of the selected stock's valuation data.
 *
 * The values required to render the dialog are stored as Fragment arguments rather than
 * re-resolved from [com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.StockListViewModel]'s
 * current state on (re)creation. FragmentManager saves and restores Fragment arguments
 * automatically — including across process death, not just rotation — so this dialog can be
 * rebuilt correctly even if it's restored before the stock list has finished reloading from Room.
 *
 * This only stores primitive strings, not a Parcelable [StockUiModel].
 */
class StockDetailDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "StockDetailDialogFragment"
        private const val ARG_CODE = "arg_code"
        private const val ARG_NAME = "arg_name"
        private const val ARG_PE_RATIO = "arg_pe_ratio"
        private const val ARG_DIVIDEND_YIELD = "arg_dividend_yield"
        private const val ARG_PB_RATIO = "arg_pb_ratio"

        fun newInstance(stock: StockUiModel): StockDetailDialogFragment =
            StockDetailDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_CODE, stock.code)
                        putString(ARG_NAME, stock.name)
                        putString(ARG_PE_RATIO, stock.peRatio)
                        putString(ARG_DIVIDEND_YIELD, stock.dividendYield)
                        putString(ARG_PB_RATIO, stock.pbRatio)
                    }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val code = args.requireString(ARG_CODE)
        val name = args.requireString(ARG_NAME)
        val peRatio = args.requireString(ARG_PE_RATIO)
        val dividendYield = args.requireString(ARG_DIVIDEND_YIELD)
        val pbRatio = args.requireString(ARG_PB_RATIO)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.stock_detail_title, name, code))
            .setMessage(
                listOf(
                    getString(R.string.stock_detail_pe_ratio, peRatio),
                    getString(R.string.stock_detail_dividend_yield, dividendYield),
                    getString(R.string.stock_detail_pb_ratio, pbRatio),
                ).joinToString("\n"),
            ).setPositiveButton(R.string.dialog_confirm, null)
            .create()
    }

    private fun Bundle.requireString(key: String): String =
        requireNotNull(getString(key)) { "Missing required Fragment argument: $key" }
}
