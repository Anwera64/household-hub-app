package com.homelab.household.app.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.icons.HearthIconImage
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.a11y_secret_off
import com.homelab.household.app.resources.a11y_secret_on
import com.homelab.household.app.resources.secret_marker
import com.homelab.household.app.resources.secret_pill_off
import com.homelab.household.app.resources.secret_pill_on
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthTheme
import org.jetbrains.compose.resources.stringResource

/**
 * The header's Secret Mode pill (canvas Conversation, SecretDay/SecretNight). OFF is an outline
 * that turns it on; ON wears the achromatic ghost and does nothing yet, since turning it off with
 * its confirmation comes in P2.
 */
@Composable
internal fun SecretPill(
    on: Boolean,
    onTurnOn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HearthTheme.colors
    val description = stringResource(if (on) Res.string.a11y_secret_on else Res.string.a11y_secret_off)
    val tint = if (on) colors.secret else colors.textMuted
    Row(
        modifier = modifier
            .heightIn(min = HearthTheme.size.touchTarget)
            .padding(end = HearthTheme.spacing.md)
            .clip(HearthShapes.pill)
            .then(if (on) Modifier else Modifier.clickable(onClick = onTurnOn))
            .clearAndSetSemantics {
                contentDescription = description
                if (!on) role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(HearthShapes.pill)
                .then(
                    if (on) {
                        Modifier
                            .background(colors.secret.copy(alpha = colors.ghostTint))
                            .border(
                                HearthTheme.size.hairline,
                                colors.secret.copy(alpha = colors.ghostEdge),
                                HearthShapes.pill,
                            )
                    } else {
                        Modifier.border(HearthTheme.size.hairline, colors.outline, HearthShapes.pill)
                    },
                )
                .padding(horizontal = HearthTheme.spacing.md, vertical = HearthTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HearthIconImage(
                icon = if (on) HearthIcon.SecretLocked else HearthIcon.SecretOpen,
                contentDescription = null,
                size = HearthTheme.size.iconSm,
                tint = tint,
            )
            Text(
                text = stringResource(if (on) Res.string.secret_pill_on else Res.string.secret_pill_off),
                style = HearthTheme.typography.labelStrong,
                color = tint,
            )
        }
    }
}

/**
 * Where the chat turned secret: a hairline with the lock and the promise in the middle, like the
 * look-over line, in the Secret Mode grey.
 */
@Composable
internal fun SecretMarker(modifier: Modifier = Modifier) {
    val colors = HearthTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HearthTheme.size.touchTarget),
        horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outlineSoft)
        HearthIconImage(
            icon = HearthIcon.SecretLocked,
            contentDescription = null,
            size = HearthTheme.size.iconSm,
            tint = colors.secret,
        )
        Text(stringResource(Res.string.secret_marker), style = HearthTheme.typography.caption, color = colors.secret)
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outlineSoft)
    }
}
