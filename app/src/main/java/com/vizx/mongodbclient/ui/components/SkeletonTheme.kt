package com.vizx.mongodbclient.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.vizx.mongodbclient.data.AppTheme

/**
 * Design tokens and theme palettes for the skeleton UI.
 * Powered by Compose mutableStateOf for instant, reactive theme switching
 * without violating the pure monospace skeleton paradigm.
 */
data class SkeletonColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderFocused: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val buttonPrimary: Color,
    val buttonSuccess: Color,
    val buttonDanger: Color
)

val TerminalDarkColors = SkeletonColors(
    background = Color(0xFF101010),
    surface = Color(0xFF1A1A1A),
    surfaceElevated = Color(0xFF222222),
    border = Color(0xFF333333),
    borderFocused = Color(0xFF555555),
    textPrimary = Color(0xFFEEEEEE),
    textSecondary = Color(0xFF888888),
    textDisabled = Color(0xFF555555),
    success = Color(0xFF4CAF50),
    warning = Color(0xFFFFB74D),
    error = Color(0xFFE57373),
    info = Color(0xFF64B5F6),
    buttonPrimary = Color(0xFF1976D2),
    buttonSuccess = Color(0xFF2E7D32),
    buttonDanger = Color(0xFF7A1C1C)
)

val OledBlackColors = SkeletonColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0C0C0C),
    surfaceElevated = Color(0xFF161616),
    border = Color(0xFF282828),
    borderFocused = Color(0xFF666666),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA0A0A0),
    textDisabled = Color(0xFF505050),
    success = Color(0xFF00E676),
    warning = Color(0xFFFFAB00),
    error = Color(0xFFFF5252),
    info = Color(0xFF40C4FF),
    buttonPrimary = Color(0xFF2979FF),
    buttonSuccess = Color(0xFF00C853),
    buttonDanger = Color(0xFFD50000)
)

val CrtAmberColors = SkeletonColors(
    background = Color(0xFF120C00),
    surface = Color(0xFF1F1400),
    surfaceElevated = Color(0xFF2E1E02),
    border = Color(0xFF593B05),
    borderFocused = Color(0xFF996600),
    textPrimary = Color(0xFFFFB000),
    textSecondary = Color(0xFFCC8C00),
    textDisabled = Color(0xFF734E00),
    success = Color(0xFFFFCC00),
    warning = Color(0xFFFF8800),
    error = Color(0xFFFF3300),
    info = Color(0xFFFFD700),
    buttonPrimary = Color(0xFF996600),
    buttonSuccess = Color(0xFFB37400),
    buttonDanger = Color(0xFF801A00)
)

val PaperLightColors = SkeletonColors(
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEAECEF),
    border = Color(0xFFD0D7DE),
    borderFocused = Color(0xFF0969DA),
    textPrimary = Color(0xFF1F2328),
    textSecondary = Color(0xFF656D76),
    textDisabled = Color(0xFF8C959F),
    success = Color(0xFF1A7F37),
    warning = Color(0xFF9A6700),
    error = Color(0xFFCF222E),
    info = Color(0xFF0969DA),
    buttonPrimary = Color(0xFF0969DA),
    buttonSuccess = Color(0xFF1F883D),
    buttonDanger = Color(0xFFCF222E)
)

object SkeletonTheme {
    var colors by mutableStateOf(TerminalDarkColors)

    fun setTheme(theme: AppTheme) {
        colors = when (theme) {
            AppTheme.TERMINAL_DARK -> TerminalDarkColors
            AppTheme.OLED_BLACK -> OledBlackColors
            AppTheme.CRT_AMBER -> CrtAmberColors
            AppTheme.PAPER_LIGHT -> PaperLightColors
        }
    }

    val Background: Color get() = colors.background
    val Surface: Color get() = colors.surface
    val SurfaceElevated: Color get() = colors.surfaceElevated
    val Border: Color get() = colors.border
    val BorderFocused: Color get() = colors.borderFocused

    val TextPrimary: Color get() = colors.textPrimary
    val TextSecondary: Color get() = colors.textSecondary
    val TextDisabled: Color get() = colors.textDisabled

    val Success: Color get() = colors.success
    val Warning: Color get() = colors.warning
    val Error: Color get() = colors.error
    val Info: Color get() = colors.info

    val ButtonPrimary: Color get() = colors.buttonPrimary
    val ButtonSuccess: Color get() = colors.buttonSuccess
    val ButtonDanger: Color get() = colors.buttonDanger
}
