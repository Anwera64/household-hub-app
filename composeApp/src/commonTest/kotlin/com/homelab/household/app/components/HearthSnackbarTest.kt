package com.homelab.household.app.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.testing.StillTheme
import kotlin.test.Test

/** A short line at the bottom of the screen after something changed out of sight. */
@OptIn(ExperimentalTestApi::class)
class HearthSnackbarTest {
    @Test
    fun `GIVEN a snackbar WHEN it is shown THEN its message reads in full at a size big enough to hit`() =
        runComposeUiTest {
            setContent { StillTheme { HearthSnackbar(message = MESSAGE, icon = HearthIcon.Synced) } }

            onHearthSnackbar { seesTheMessage(MESSAGE) }
        }

    @Test
    fun `GIVEN a snackbar WHEN it is shown THEN a screen reader reads it politely`() =
        runComposeUiTest {
            setContent { StillTheme { HearthSnackbar(message = MESSAGE) } }

            onHearthSnackbar { hearsItPolitely(MESSAGE) }
        }

    private companion object {
        const val MESSAGE = "You're back on home Wi-Fi."
    }
}
