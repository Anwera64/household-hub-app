package com.homelab.household.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.icons.HearthIconImage
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthTheme

/** What a snackbar's icon says about the news: settled, or something still to do. */
enum class SnackbarTone { Success, Attention }

/** A snackbar's words and icon, for [HearthSnackbarHost]. Material's default short duration. */
data class HearthSnackbarVisuals(
    override val message: String,
    val icon: HearthIcon? = null,
    val tone: SnackbarTone = SnackbarTone.Success,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/**
 * A short line after something changed out of sight: dark on the light canvas (inverted at night),
 * at least a touch target tall. Read out politely, so it doesn't cut a screen reader off.
 */
@Composable
fun HearthSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    icon: HearthIcon? = null,
    iconTint: Color = HearthTheme.colors.canvas,
) {
    val colors = HearthTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HearthTheme.size.touchTarget)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .background(colors.textPrimary, HearthShapes.item)
            .padding(horizontal = HearthTheme.spacing.lg, vertical = HearthTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.md),
    ) {
        if (icon != null) {
            HearthIconImage(icon = icon, contentDescription = null, size = HearthTheme.size.iconMd, tint = iconTint)
        }
        Text(message, style = HearthTheme.typography.body, color = colors.canvas)
    }
}

/**
 * The app's one snackbar host, over everything [content] draws: [HearthTheme.spacing.xl] in from
 * the sides, and lifted above the bottom bar and its raised + so it never covers a tab.
 */
@Composable
fun HearthSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                )
                .padding(horizontal = HearthTheme.spacing.xl)
                .padding(bottom = bottomBarClearance()),
        ) { data ->
            val visuals = data.visuals as? HearthSnackbarVisuals
            HearthSnackbar(
                message = data.visuals.message,
                icon = visuals?.icon,
                iconTint = visuals?.tone?.let { toneTint(it) } ?: HearthTheme.colors.canvas,
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun toneTint(tone: SnackbarTone): Color =
    when (tone) {
        SnackbarTone.Success -> HearthTheme.colors.successContainer
        SnackbarTone.Attention -> HearthTheme.colors.tertiaryContainer
    }

/**
 * The bottom bar's height (its top and bottom padding around a touch-target tab), the raised +
 * standing [HearthTheme.spacing.xxl] above it, and the snackbar's own [HearthTheme.spacing.xl].
 */
@Composable
@ReadOnlyComposable
private fun bottomBarClearance(): Dp {
    val spacing = HearthTheme.spacing
    return spacing.md + HearthTheme.size.touchTarget + spacing.lg + spacing.xxl + spacing.xl
}
