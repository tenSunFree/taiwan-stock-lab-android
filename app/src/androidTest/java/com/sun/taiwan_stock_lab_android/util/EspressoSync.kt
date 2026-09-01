package com.sun.taiwan_stock_lab_android.util

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.NoMatchingViewException

/**
 * The polling mechanism provided by ComposeTestRule: repeatedly executes the assertion until the condition is met or a timeout occurs.
 * Used to wait for asynchronous screen updates such as Room/Flow/Paging diffs, which Espresso does not automatically synchronize.
 * Only swallow exceptions where the assertion is not met (View not found/assertion failed). Other exceptions (NPE, ISE, etc.) should be explicitly displayed.
 * Otherwise, the actual program error will be swallowed, and you will only see a difficult-to-locate timeout.
 */
fun ComposeTestRule.waitUntilEspressoAssertionPasses(
    timeoutMillis: Long = 5_000L,
    assertion: () -> Unit,
) {
    waitUntil(timeoutMillis) {
        try {
            assertion()
            true
        } catch (_: AssertionError) {
            false
        } catch (_: NoMatchingViewException) {
            false
        }
    }
}
