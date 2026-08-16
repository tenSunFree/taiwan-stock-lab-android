package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sun.taiwan_stock_lab_android.core.ui.R as CoreUiR
import com.sun.taiwan_stock_lab_android.feature.stocklist.databinding.ItemStockCardBinding
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.ChangeDirection
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.PricePosition
import com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.model.StockUiModel

class StockListAdapter(
    private val onStockClicked: (String) -> Unit,
) : ListAdapter<StockUiModel, StockListAdapter.StockViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding =
            ItemStockCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StockViewHolder(binding, onStockClicked)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StockViewHolder(
        private val binding: ItemStockCardBinding,
        private val onStockClicked: (String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: StockUiModel) = with(binding) {
            textCode.text = stock.code
            textName.text = stock.name
            textOpeningPrice.text = stock.openingPrice
            textHighestPrice.text = stock.highestPrice
            textLowestPrice.text = stock.lowestPrice
            textMonthlyAverage.text = stock.monthlyAveragePrice
            textTransactionCount.text = stock.transactionCount
            textTradeVolume.text = stock.tradeVolume
            textTradeValue.text = stock.tradeValue
            textClosingPrice.text = stock.closingPrice
            textClosingPrice.setTextColor(colorForPosition(stock.closingPricePosition))
            textChange.text = stock.change
            textChange.setTextColor(colorForDirection(stock.changeDirection))
            root.setOnClickListener { onStockClicked(stock.code) }
        }

        private fun colorForPosition(position: PricePosition): Int {
            val colorRes = when (position) {
                PricePosition.ABOVE_AVERAGE -> CoreUiR.color.stock_price_up
                PricePosition.BELOW_AVERAGE -> CoreUiR.color.stock_price_down
                PricePosition.EQUAL, PricePosition.UNKNOWN -> android.R.color.darker_gray
            }
            return ContextCompat.getColor(binding.root.context, colorRes)
        }

        private fun colorForDirection(direction: ChangeDirection): Int {
            val colorRes = when (direction) {
                ChangeDirection.POSITIVE -> CoreUiR.color.stock_price_up
                ChangeDirection.NEGATIVE -> CoreUiR.color.stock_price_down
                ChangeDirection.ZERO, ChangeDirection.UNKNOWN -> android.R.color.darker_gray
            }
            return ContextCompat.getColor(binding.root.context, colorRes)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<StockUiModel>() {
        override fun areItemsTheSame(oldItem: StockUiModel, newItem: StockUiModel) =
            oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: StockUiModel, newItem: StockUiModel) =
            oldItem == newItem
    }
}