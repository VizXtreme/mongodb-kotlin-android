package com.vizx.mongodbclient.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Design tokens for the skeleton UI.
 * Keeping colors and styling separate allows instant re-theming or migration
 * to full Material 3 / custom themes without touching business logic or screen code.
 */
object SkeletonTheme {
    val Background = Color(0xFF101010)
    val Surface = Color(0xFF1A1A1A)
    val SurfaceElevated = Color(0xFF222222)
    val Border = Color(0xFF333333)
    val BorderFocused = Color(0xFF555555)

    val TextPrimary = Color(0xFFEEEEEE)
    val TextSecondary = Color(0xFF888888)
    val TextDisabled = Color(0xFF555555)

    // Functional indicators
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFB74D)
    val Error = Color(0xFFE57373)
    val Info = Color(0xFF64B5F6)

    // Button container colors
    val ButtonPrimary = Color(0xFF1976D2)
    val ButtonSuccess = Color(0xFF2E7D32)
    val ButtonDanger = Color(0xFF7A1C1C)
}
