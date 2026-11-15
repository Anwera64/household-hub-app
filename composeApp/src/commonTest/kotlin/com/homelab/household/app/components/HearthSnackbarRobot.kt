package com.homelab.household.app.components

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.homelab.household.app.theme.DefaultSizes

/** What a person sees and hears of a snackbar. */
@OptIn(ExperimentalTestApi::class)
class HearthSnackbarRobot(
    private val test: ComposeUiTest,
) {
    fun seesTheMessage(message: String) {
        test.waitUntilExactlyOneExists(hasText(message), timeoutMillis = WAIT_MILLIS)
        test
            .onNodeWithText(message)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(DefaultSizes.touchTarget)
    }

    /** Read out when it appears, without cutting off what the screen reader was saying. */
    fun hearsItPolitely(message: String) {
        test
            .onNodeWithText(message)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.onHearthSnackbar(block: HearthSnackbarRobot.() -> Unit) {
    HearthSnackbarRobot(this).block()
}
