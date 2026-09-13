package com.sun.taiwan_stock_lab_android.feature.stocklist.presentation.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Minimal reproduction proving the NPE hit when constructing StockListAdapter under Robolectric
 * is an environment-level issue, not a StockListAdapter defect. canCallSetStateRestorationPolicy
 * below reproduces the identical NullPointerException on a plain RecyclerView.Adapter with zero
 * Paging involvement, by calling adapter.stateRestorationPolicy = ... directly — the exact call
 * RecyclerView.Adapter.setStateRestorationPolicy() makes internally.
 *
 * NOTE: confirmed to reproducibly NPE under Robolectric 4.15.1 AND 4.16.1. NOT yet confirmed to
 * be specific to androidx.recyclerview:recyclerview 1.4.0 — that would require re-running this
 * with an older recyclerview version forced on the test classpath, which hasn't been done. Don't
 * overstate the conclusion beyond "reproducibly NPEs in the current Robolectric JVM test
 * environment" until that control test is run.
 *
 * Kept long-term as the reference repro for this issue — do not delete when re-investigating.
 * See StockViewHolderTest's kdoc for how the real coverage work routes around this.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecyclerViewSanityTest {
    private class FakeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder = error("unused")

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) = Unit

        override fun getItemCount(): Int = 0
    }

    @Test
    fun canConstructPlainAdapter() {
        FakeAdapter()
    }

    /**
     * This is the actual call PagingDataAdapter's constructor makes internally
     * (setStateRestorationPolicy(StateRestorationPolicy.PREVENT)). canConstructPlainAdapter above
     * only proves a bare Adapter() constructs fine — it never exercises this specific method,
     * which is where the original NPE actually originates. This test isolates that call on its
     * own, with no Paging involved at all, and is intentionally left @Ignore'd so it documents
     * the repro without failing the build every run.
     */
    @Ignore(
        "Reference repro — RecyclerView.Adapter.setStateRestorationPolicy() NPEs under Robolectric. See class kdoc.",
    )
    @Test
    fun canCallSetStateRestorationPolicy() {
        val adapter = FakeAdapter()
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT
    }
}
