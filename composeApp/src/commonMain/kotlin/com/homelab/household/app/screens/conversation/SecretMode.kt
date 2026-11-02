package com.homelab.household.app.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.homelab.household.app.components.ActionCard
import com.homelab.household.app.components.CardButtonRow
import com.homelab.household.app.components.PrimaryButton
import com.homelab.household.app.components.SecondaryButton
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.icons.HearthIconImage
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.a11y_secret_off
import com.homelab.household.app.resources.a11y_secret_on
import com.homelab.household.app.resources.secret_kept_private
import com.homelab.household.app.resources.secret_marker
import com.homelab.household.app.resources.secret_marker_from_here
import com.homelab.household.app.resources.secret_offer_accept
import com.homelab.household.app.resources.secret_offer_agent
import com.homelab.household.app.resources.secret_offer_judged
import com.homelab.household.app.resources.secret_offer_not_now
import com.homelab.household.app.resources.secret_offer_title
import com.homelab.household.app.resources.secret_pill_off
import com.homelab.household.app.resources.secret_pill_on
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthTheme
import com.homelab.household.presentation.chatsession.SecretOffer
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
 * look-over line, in the Secret Mode grey. [fromHere] when ordinary messages come before it: what
 * was said there stays shared, so it promises only "from here on" (canvas: PrivacyNowSecret).
 */
@Composable
internal fun SecretMarker(
    fromHere: Boolean,
    modifier: Modifier = Modifier,
) {
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
        Text(
            text = stringResource(if (fromHere) Res.string.secret_marker_from_here else Res.string.secret_marker),
            style = HearthTheme.typography.caption,
            color = colors.secret,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outlineSoft)
    }
}

/**
 * Under a question the hub kept private, in place of its Sent receipt: the lock and that it stays
 * between the member and the agent, in the Secret Mode grey (canvas: PrivacyPrompt). The lock is
 * the receipt's size, the set's smallest that keeps its contrast.
 */
@Composable
internal fun KeptPrivateLine(modifier: Modifier = Modifier) {
    val colors = HearthTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HearthIconImage(
            icon = HearthIcon.SecretLocked,
            contentDescription = null,
            size = HearthTheme.size.iconSm,
            tint = colors.secret,
        )
        Text(
            stringResource(Res.string.secret_kept_private),
            style = HearthTheme.typography.caption,
            color = colors.secret,
        )
    }
}

/**
 * After an answer to a private message: the offer to make the whole chat secret (canvas:
 * PrivacyPrompt, PrivacyAgentOffer). Its words say who noticed: the hub's check, or the agent.
 */
@Composable
internal fun SecretOfferCard(
    offer: SecretOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HearthTheme.colors
    val type = HearthTheme.typography
    val detail =
        when (offer) {
            SecretOffer.Judged -> Res.string.secret_offer_judged
            SecretOffer.Agent -> Res.string.secret_offer_agent
        }
    ActionCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.sm)) {
            // Nudged down to sit on the title's first line, as the canvas draws it.
            HearthIconImage(
                icon = HearthIcon.SecretLocked,
                contentDescription = null,
                size = HearthTheme.size.iconSm,
                tint = colors.secret,
                modifier = Modifier.padding(top = HearthTheme.spacing.xxs),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HearthTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(Res.string.secret_offer_title),
                    style = type.bodyStrong,
                    color = colors.textPrimary,
                )
                Text(text = stringResource(detail), style = type.caption, color = colors.textMuted)
            }
        }
        CardButtonRow {
            SecondaryButton(
                text = stringResource(Res.string.secret_offer_not_now),
                onClick = onDecline,
                compact = true,
            )
            PrimaryButton(
                text = stringResource(Res.string.secret_offer_accept),
                onClick = onAccept,
                modifier = Modifier.weight(1f),
                lifted = false,
                compact = true,
            )
        }
    }
}
