package com.homelab.household.app.components

import androidx.compose.runtime.Composable
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.theme.HearthTheme
import com.homelab.household.app.theme.PreviewDayNight

/** The two snackbars the local network screen leaves behind: settled, and something still to do. */
@PreviewDayNight
@Composable
private fun HearthSnackbarPreview() {
    ComponentPreview {
        HearthSnackbar(
            message = "You're back on home Wi-Fi.",
            icon = HearthIcon.Synced,
            iconTint = HearthTheme.colors.successContainer,
        )
        HearthSnackbar(
            message = "You can turn it on later in Profile.",
            icon = HearthIcon.Settings,
            iconTint = HearthTheme.colors.tertiaryContainer,
        )
    }
}
