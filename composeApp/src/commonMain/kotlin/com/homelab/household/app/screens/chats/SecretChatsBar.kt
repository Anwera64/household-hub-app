package com.homelab.household.app.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.homelab.household.app.components.PrimaryButton
import com.homelab.household.app.components.SecondaryButton
import com.homelab.household.app.components.secretOutline
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.icons.HearthIconImage
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.chats_secret_lock_now
import com.homelab.household.app.resources.chats_secret_locked
import com.homelab.household.app.resources.chats_secret_locked_line
import com.homelab.household.app.resources.chats_secret_unlock
import com.homelab.household.app.resources.chats_secret_unlocked
import com.homelab.household.app.resources.chats_secret_unlocked_line
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthTheme
import com.homelab.household.presentation.chats.SecretBar
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The bar over the Chats list (canvas Chats and ChatsUnlocked): Unlock opens every secret chat with
 * one PIN, and once they are open Lock now closes them again. No countdown: the ten minutes slide
 * with every touch, so a number would only ever read 10:00.
 */
@Composable
fun SecretChatsBar(
    bar: SecretBar,
    onUnlock: () -> Unit,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HearthTheme.colors
    val type = HearthTheme.typography
    val locked = bar is SecretBar.Locked

    Row(
        modifier = modifier
            .fillMaxWidth()
            .secretOutline()
            .padding(vertical = HearthTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(HearthTheme.size.touchTarget)
                .clip(HearthShapes.item)
                .background(colors.secret.copy(alpha = colors.ghostTint))
                .border(HearthTheme.size.hairline, colors.secret.copy(alpha = colors.ghostEdge), HearthShapes.item),
            contentAlignment = Alignment.Center,
        ) {
            HearthIconImage(
                icon = if (locked) HearthIcon.SecretLocked else HearthIcon.SecretOpen,
                contentDescription = null,
                size = HearthTheme.size.iconMd,
                tint = colors.secret,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(HearthTheme.spacing.xxs),
        ) {
            Text(
                text =
                    when (bar) {
                        is SecretBar.Locked -> pluralStringResource(
                            Res.plurals.chats_secret_locked,
                            bar.count,
                            bar.count,
                        )

                        SecretBar.Unlocked -> stringResource(Res.string.chats_secret_unlocked)
                    },
                style = type.bodyStrong,
                color = colors.textPrimary,
            )
            Text(
                text =
                    stringResource(
                        if (locked) Res.string.chats_secret_locked_line else Res.string.chats_secret_unlocked_line,
                    ),
                style = type.caption,
                color = colors.textMuted,
            )
        }
        if (locked) {
            PrimaryButton(
                text = stringResource(Res.string.chats_secret_unlock),
                onClick = onUnlock,
                lifted = false,
                compact = true,
            )
        } else {
            SecondaryButton(
                text = stringResource(Res.string.chats_secret_lock_now),
                onClick = onLockNow,
                compact = true,
            )
        }
    }
}
