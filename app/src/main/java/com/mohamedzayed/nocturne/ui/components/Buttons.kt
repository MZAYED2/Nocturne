package com.mohamedzayed.nocturne.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedzayed.nocturne.ui.theme.NocturneTheme

@Composable
fun MidnightPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = NocturneTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primaryAccent,
            contentColor = colors.mainBackground,
            disabledContainerColor = colors.primaryAccent.copy(alpha = 0.35f),
            disabledContentColor = colors.mainBackground.copy(alpha = 0.5f)
        ),
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun MidnightSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = NocturneTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.elevatedSurface,
            contentColor = colors.primaryText,
            disabledContainerColor = colors.elevatedSurface.copy(alpha = 0.4f),
            disabledContentColor = colors.primaryText.copy(alpha = 0.4f)
        ),
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun MidnightOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = NocturneTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) colors.primaryAccent.copy(alpha = 0.6f) else colors.primaryAccent.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.primaryAccent,
            disabledContentColor = colors.primaryAccent.copy(alpha = 0.35f)
        ),
        contentPadding = contentPadding,
        content = content
    )
}
