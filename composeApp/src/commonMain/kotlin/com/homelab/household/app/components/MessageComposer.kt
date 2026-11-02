package com.homelab.household.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.icons.HearthIconImage
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.send_message
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthSizes
import com.homelab.household.app.theme.HearthTheme
import org.jetbrains.compose.resources.stringResource

/** Tags a message box drawn with the dashed outline of a secret chat. */
const val COMPOSER_DASHED_TAG = "composer-dashed"

/**
 * The message box under a conversation. Stateless: sending never clears the text here —
 * the screen decides when a message has really gone. Send is never disabled.
 *
 * In a secret chat ([dashed]) its outline is dashed in the Secret Mode grey: the one sign at the
 * edge of the screen, and it sits where you type (canvas "Secret look · 5", chosen 2 Nov 2026).
 */
@Composable
fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    dashed: Boolean = false,
) {
    val colors = HearthTheme.colors
    val textStyle = HearthTheme.typography.label

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            // Bottom and horizontal only: the keyboard is already in `safeDrawing`, so this
            // rises above it rather than hiding underneath.
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        HorizontalDivider(thickness = HearthTheme.size.hairline, color = colors.outlineSoft)
        Row(
            modifier =
                Modifier.padding(
                    start = HearthTheme.spacing.lg,
                    top = HearthTheme.spacing.md,
                    end = HearthTheme.spacing.lg,
                    bottom = HearthTheme.spacing.lg,
                ),
            horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = textStyle.copy(color = colors.textPrimary),
                maxLines = 5,
                // The keyboard's own key sends. It says "Send" rather than offering a newline,
                // which is the whole point: a message is a message, and reaching for the button
                // after typing one is a motion too many. Long messages still wrap to five lines.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend(value) }),
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { innerField ->
                    Box(
                        modifier = Modifier
                            .background(colors.canvas, HearthShapes.pill)
                            .then(
                                if (dashed) {
                                    Modifier
                                        .dashedPillOutline(
                                            colors.secret.copy(alpha = colors.ghostEdge),
                                            HearthTheme.size,
                                        )
                                        .testTag(COMPOSER_DASHED_TAG)
                                } else {
                                    Modifier.border(HearthTheme.size.hairline, colors.outline, HearthShapes.pill)
                                },
                            )
                            .padding(horizontal = HearthTheme.spacing.lg, vertical = HearthTheme.spacing.md),
                    ) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = textStyle, color = colors.textMuted)
                        }
                        innerField()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(HearthTheme.size.touchTarget)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable(role = Role.Button) { onSend(value) },
                contentAlignment = Alignment.Center,
            ) {
                HearthIconImage(
                    icon = HearthIcon.Send,
                    contentDescription = stringResource(Res.string.send_message),
                    active = true,
                    size = HearthTheme.size.iconMd,
                    tint = colors.onPrimary,
                )
            }
        }
    }
}

/**
 * A pill's outline drawn in dashes. Compose's own border only draws solid lines, so the stroke is
 * drawn here, inset by half its width so it sits inside the pill as the solid one does.
 */
private fun Modifier.dashedPillOutline(
    color: Color,
    sizes: HearthSizes,
): Modifier =
    drawBehind {
        val width = sizes.dashedOutline.toPx()
        val inset = width / 2
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - width, size.height - width),
            cornerRadius = CornerRadius((size.height - width) / 2),
            style =
                Stroke(
                    width = width,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(sizes.dash.toPx(), sizes.dashGap.toPx())),
                ),
        )
    }
