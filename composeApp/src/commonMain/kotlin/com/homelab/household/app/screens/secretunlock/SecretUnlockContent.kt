package com.homelab.household.app.screens.secretunlock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.homelab.household.app.components.HearthScaffold
import com.homelab.household.app.components.HearthTopBar
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.icons.HearthIconImage
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.conversation_back
import com.homelab.household.app.resources.secret_locked_line
import com.homelab.household.app.resources.secret_locked_not_a_safe
import com.homelab.household.app.resources.secret_locked_title
import com.homelab.household.app.screens.pinentry.PinPad
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthTheme
import com.homelab.household.presentation.secretunlock.SecretUnlockUiState
import org.jetbrains.compose.resources.stringResource

/**
 * A secret chat with nothing unlocked (canvas SecretUnlock): the sign-in PIN pad with a lock tile
 * in place of the avatar, and no composer. The right PIN opens every secret chat for ten minutes.
 *
 * Stateless — [SecretUnlockScreen] owns the ViewModel.
 */
@Composable
fun SecretUnlockContent(
    state: SecretUnlockUiState,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HearthScaffold(
        modifier = modifier,
        header = { HearthTopBar(onBack = onBack, backDescription = stringResource(Res.string.conversation_back)) },
        gutter = HearthTheme.spacing.none,
    ) { padding ->
        PinPad(
            entered = state.entered,
            status = state.status,
            face = { LockTile() },
            title = stringResource(Res.string.secret_locked_title),
            lines =
                listOf(
                    stringResource(Res.string.secret_locked_line),
                    stringResource(Res.string.secret_locked_not_a_safe),
                ),
            onDigit = onDigit,
            onDelete = onDelete,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

/** The lock in the Secret Mode ghost, where the sign-in pad has the member's face. */
@Composable
private fun LockTile() {
    val colors = HearthTheme.colors
    Box(
        modifier = Modifier
            .size(HearthTheme.size.tile)
            .clip(HearthShapes.tile)
            .background(colors.secret.copy(alpha = colors.ghostTint))
            .border(HearthTheme.size.hairline, colors.secret.copy(alpha = colors.ghostEdge), HearthShapes.tile),
        contentAlignment = Alignment.Center,
    ) {
        HearthIconImage(
            icon = HearthIcon.SecretLocked,
            contentDescription = null,
            size = HearthTheme.size.iconXl,
            tint = colors.secret,
        )
    }
}
