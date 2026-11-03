package com.homelab.household.app.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import com.homelab.household.app.components.HearthCheckbox
import com.homelab.household.app.components.PrimaryButton
import com.homelab.household.app.components.SecondaryButton
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.icons.HearthIconImage
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.a11y_secret_off
import com.homelab.household.app.resources.a11y_secret_on
import com.homelab.household.app.resources.secret_marker
import com.homelab.household.app.resources.secret_off_confirm
import com.homelab.household.app.resources.secret_off_dont_ask
import com.homelab.household.app.resources.secret_off_from_here
import com.homelab.household.app.resources.secret_off_keep
import com.homelab.household.app.resources.secret_off_stays_locked
import com.homelab.household.app.resources.secret_off_title
import com.homelab.household.app.resources.secret_part_locked
import com.homelab.household.app.resources.secret_part_title
import com.homelab.household.app.resources.secret_part_unlock
import com.homelab.household.app.resources.secret_pill_off
import com.homelab.household.app.resources.secret_pill_on
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The header's Secret Mode pill (canvas Conversation, SecretDay/SecretNight). OFF is an outline
 * that turns it on; ON wears the achromatic ghost and turns it off, after the sheet (SecretTurnOff).
 */
@Composable
internal fun SecretPill(
    on: Boolean,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
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
            .clickable(onClick = if (on) onTurnOff else onTurnOn)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
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
 * Where the chat turned secret, or back: a hairline with the lock and [text] in the middle, like the
 * look-over line. The live part's promise and an earlier part's "Secret from here on" are in the
 * Secret Mode grey; "No longer secret from here" ([ended]) is in the muted text colour.
 */
@Composable
internal fun SecretMarker(
    modifier: Modifier = Modifier,
    text: StringResource = Res.string.secret_marker,
    ended: Boolean = false,
) {
    val colors = HearthTheme.colors
    val tint = if (ended) colors.textMuted else colors.secret
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
            tint = tint,
        )
        Text(stringResource(text), style = HearthTheme.typography.caption, color = tint)
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.outlineSoft)
    }
}

/**
 * An earlier secret part the hub withheld, folded into one band in the ghost (SecretTurnedOff):
 * how many messages it holds, and Unlock, which asks for the PIN and opens them in place.
 */
@Composable
internal fun LockedSecretPart(
    messages: Int,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HearthTheme.colors
    val spacing = HearthTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(HearthShapes.item)
            .background(colors.secret.copy(alpha = colors.ghostTint))
            .border(HearthTheme.size.hairline, colors.secret.copy(alpha = colors.ghostEdge), HearthShapes.item)
            .padding(start = spacing.lg, top = spacing.md, end = spacing.md, bottom = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HearthIconImage(
            icon = HearthIcon.SecretLocked,
            contentDescription = null,
            size = HearthTheme.size.iconMd,
            tint = colors.secret,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                pluralStringResource(Res.plurals.secret_part_title, messages, messages),
                style = HearthTheme.typography.bodyStrong,
                color = colors.textPrimary,
            )
            Text(
                stringResource(Res.string.secret_part_locked),
                style = HearthTheme.typography.caption,
                color = colors.textMuted,
            )
        }
        Box(
            modifier = Modifier
                .heightIn(min = HearthTheme.size.touchTarget)
                .clip(HearthShapes.button)
                .clickable(role = Role.Button, onClick = onUnlock)
                .padding(horizontal = spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.secret_part_unlock),
                style = HearthTheme.typography.bodyStrong,
                color = colors.primary,
            )
        }
    }
}

/**
 * Asks before Secret Mode turns off (SecretTurnOff): what changes from here, that what was said in
 * secret stays locked, and "Don't ask again" for this member on this phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecretOffSheet(
    onKeep: () -> Unit,
    onTurnOff: (dontAskAgain: Boolean) -> Unit,
) {
    val colors = HearthTheme.colors
    val type = HearthTheme.typography
    val spacing = HearthTheme.spacing
    var dontAskAgain by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onKeep,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = HearthShapes.sheet,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) },
    ) {
        Column(
            modifier = Modifier.padding(start = spacing.xxl, end = spacing.xxl, bottom = spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(stringResource(Res.string.secret_off_title), style = type.title, color = colors.textPrimary)
                Text(stringResource(Res.string.secret_off_from_here), style = type.body, color = colors.textMuted)
                Text(stringResource(Res.string.secret_off_stays_locked), style = type.body, color = colors.textMuted)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HearthTheme.size.touchTarget)
                    .toggleable(value = dontAskAgain, role = Role.Checkbox, onValueChange = { dontAskAgain = it }),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HearthCheckbox(checked = dontAskAgain)
                Text(stringResource(Res.string.secret_off_dont_ask), style = type.body, color = colors.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SecondaryButton(
                    text = stringResource(Res.string.secret_off_keep),
                    onClick = onKeep,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(Res.string.secret_off_confirm),
                    onClick = { onTurnOff(dontAskAgain) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
