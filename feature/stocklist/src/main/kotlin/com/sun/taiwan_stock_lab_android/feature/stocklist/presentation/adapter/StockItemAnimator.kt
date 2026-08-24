package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.adapter

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * A full stock-code sort reversal reorders nearly the entire dataset (1,000+ rows), which
 * [androidx.recyclerview.widget.DiffUtil] interprets as a large batch of item-move operations.
 * Animating every one of them was visually indistinguishable from the list simply scrolling on
 * its own, so move animations are skipped here.
 *
 * Ordinary add/remove/change animations (e.g. new stocks appearing on refresh, individual price
 * updates) are kept — they come from [DefaultItemAnimator] unchanged. This is a narrower
 * trade-off than disabling `itemAnimator` entirely: only the specific animation category with no
 * UX value for this data-dense financial list is turned off.
 */
class StockItemAnimator : DefaultItemAnimator() {
    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
    ): Boolean {
        dispatchMoveFinished(holder)
        return false
    }
}
