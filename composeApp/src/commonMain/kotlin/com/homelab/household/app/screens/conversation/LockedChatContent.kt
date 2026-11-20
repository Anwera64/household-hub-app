package com.homelab.household.app.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.homelab.household.app.components.HearthScaffold
import com.homelab.household.app.components.PrimaryButton
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.icons.HearthIconImage
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.secret_locked_all_line
import com.homelab.household.app.resources.secret_locked_title
import com.homelab.household.app.resources.secret_part_unlock
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthTheme
import com.homelab.household.app.theme.PreviewDayNight
import com.homelab.household.domain.model.ConversationSession
import com.homelab.household.presentation.chatsession.ChatSessionUiState
import org.jetbrains.compose.resources.stringResource

/**
 * A chat where everything is secret, locked (canvas LockedChatScreen): the chat's own top bar, a
 * lock and what happened, and Unlock, which opens the secret chats PIN pad over the chat. No
 * message box until the chat opens.
 *
 * The pill still turns Secret Mode off: the hub allows that without the unlock. A chat read whole
 * by an older hub has no session to name, and is secret all the same.
 *
 * Stateless — [ConversationScreen] owns the ViewModel.
 */
@Composable
fun LockedChatContent(
    state: ChatSessionUiState,
    onUnlock: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** The header's OFF pill, after Secret Mode was turned off while locked. */
    onTurnOnSecret: () -> Unit = {},
    /** The header's ON pill: turn Secret Mode off, after the sheet unless the member said not to ask. */
    onTurnOffSecret: () -> Unit = {},
    /** "Keep secret" on that sheet, or the sheet dismissed. */
    onKeepSecret: () -> Unit = {},
    /** "Turn off" on that sheet, with whether "Don't ask again" was ticked. */
    onConfirmSecretOff: (dontAskAgain: Boolean) -> Unit = {},
) {
    val colors = HearthTheme.colors
    val type = HearthTheme.typography
    val spacing = HearthTheme.spacing

    if (state.askingToTurnOffSecret) {
        SecretOffSheet(onKeep = onKeepSecret, onTurnOff = onConfirmSecretOff)
    }

    HearthScaffold(
        modifier = modifier,
        header = {
            ConversationTopBar(
                state = state,
                secret = state.session?.isSecret ?: true,
                onBack = onBack,
                onTurnOnSecret = onTurnOnSecret,
                onTurnOffSecret = onTurnOffSecret,
            )
        },
        gutter = spacing.none,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically),
            ) {
                LockedTile()
                Text(
                    text = stringResource(Res.string.secret_locked_title),
                    style = type.title,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(Res.string.secret_locked_all_line),
                    style = type.body,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = HearthTheme.size.readingWidth),
                )
            }
            PrimaryButton(
                text = stringResource(Res.string.secret_part_unlock),
                onClick = onUnlock,
                icon = HearthIcon.SecretOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.xl, end = spacing.xl, bottom = spacing.xxxl),
            )
        }
    }
}

/** The lock in the Secret Mode ghost. Decorative: the title says what it means. */
@Composable
private fun LockedTile() {
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
            size = HearthTheme.size.iconXxl,
            tint = colors.secret,
        )
    }
}

@PreviewDayNight
@Composable
private fun LockedChatContentPreview() {
    HearthTheme {
        LockedChatContent(
            state =
                ChatSessionUiState(
                    session = ConversationSession(id = "s-1", userId = "emma", isSecret = true),
                    agentName = "Home Coordinator",
                    agentAvatar = "🏡",
                    isLocked = true,
                ),
            onUnlock = {},
            onBack = {},
        )
    }
}
