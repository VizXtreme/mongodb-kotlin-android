package com.vizx.mongodbclient.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ButtonVariant {
    PRIMARY, SUCCESS, DANGER, OUTLINE
}

@Composable
fun SkeletonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val containerColor = when (variant) {
        ButtonVariant.PRIMARY -> SkeletonTheme.ButtonPrimary
        ButtonVariant.SUCCESS -> SkeletonTheme.ButtonSuccess
        ButtonVariant.DANGER -> SkeletonTheme.ButtonDanger
        ButtonVariant.OUTLINE -> Color.Transparent
    }

    if (variant == ButtonVariant.OUTLINE) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = RectangleShape,
            modifier = modifier.height(38.dp)
        ) {
            ButtonContent(text, isLoading)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                disabledContainerColor = SkeletonTheme.Border
            ),
            modifier = modifier.height(38.dp)
        ) {
            ButtonContent(text, isLoading)
        }
    }
}

@Composable
private fun ButtonContent(text: String, isLoading: Boolean) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = SkeletonTheme.TextPrimary,
            strokeWidth = 2.dp
        )
    } else {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SkeletonTheme.TextPrimary
        )
    }
}
