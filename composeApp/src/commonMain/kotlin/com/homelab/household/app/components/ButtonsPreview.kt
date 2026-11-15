package com.homelab.household.app.components

import androidx.compose.runtime.Composable
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.theme.PreviewDayNight

/** Each button kind, without and then with its icon. */
@PreviewDayNight
@Composable
private fun PrimaryButtonPreview() {
    ComponentPreview {
        PrimaryButton(text = "Create household", onClick = {})
        PrimaryButton(text = "New chat", onClick = {}, icon = HearthIcon.NewChat)
    }
}

@PreviewDayNight
@Composable
private fun SecondaryButtonPreview() {
    ComponentPreview {
        SecondaryButton(text = "Not now", onClick = {})
        SecondaryButton(text = "Try again", onClick = {}, icon = HearthIcon.Retry)
    }
}

@PreviewDayNight
@Composable
private fun DestructiveButtonPreview() {
    ComponentPreview {
        DestructiveButton(text = "Remove Emma", onClick = {})
        DestructiveButton(text = "Revoke access", onClick = {}, icon = HearthIcon.Revoke)
    }
}

/** The quiet text button under the two others, the way the local network screen stacks them. */
@PreviewDayNight
@Composable
private fun QuietTextButtonPreview() {
    ComponentPreview {
        PrimaryButton(text = "Open settings", onClick = {}, icon = HearthIcon.Settings)
        SecondaryButton(text = "Not now", onClick = {})
        QuietTextButton(text = "Don't ask again on this phone", onClick = {})
    }
}

/**
 * The working state of each kind, beside its resting one. Previews keep `DefaultMotion`, so the
 * bar along each bottom edge is running here — this is the one place the motion is looked at.
 */
@PreviewDayNight
@Composable
private fun BusyButtonPreview() {
    ComponentPreview {
        PrimaryButton(text = "Creating household…", onClick = {}, busy = true)
        SecondaryButton(text = "Trying again…", onClick = {}, busy = true)
        DestructiveButton(text = "Removing Emma…", onClick = {}, busy = true)
    }
}
