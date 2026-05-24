package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ReaderThemePalette {
    LAVENDER, PEACH, MINT, SKY_BLUE, CREAM, ROSE, OCEAN, SYSTEM
}

enum class ReaderModeStyle {
    STANDARD, EYE_COMFORT, DIM_LIGHT, AMOLED, WARM_PAPER, DYSLEXIA, FOCUS, NIGHT, MINIMAL_DISTRACTION
}

object ThemePalettes {
    // 1. Lavender Palette
    val LavenderLight = lightColorScheme(
        primary = Color(0xFF6E44FF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEBE5FF),
        onPrimaryContainer = Color(0xFF230066),
        secondary = Color(0xFF5F5276),
        background = Color(0xFFF9F7FC),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1D1B20),
        onSurface = Color(0xFF1D1B20)
    )
    val LavenderDark = darkColorScheme(
        primary = Color(0xFFC0B3FF),
        onPrimary = Color(0xFF260580),
        primaryContainer = Color(0xFF4C1DFF),
        secondary = Color(0xFFC7BCDF),
        background = Color(0xFF110E18),
        surface = Color(0xFF191622),
        onBackground = Color(0xFFE6E1E9),
        onSurface = Color(0xFFE6E1E9)
    )

    // 2. Peach Palette
    val PeachLight = lightColorScheme(
        primary = Color(0xFFE76F51),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFECE5),
        secondary = Color(0xFF765B54),
        background = Color(0xFFFFF7F5),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF201A18),
        onSurface = Color(0xFF201A18)
    )
    val PeachDark = darkColorScheme(
        primary = Color(0xFFFFB4A2),
        onPrimary = Color(0xFF5B1E10),
        primaryContainer = Color(0xFF7C3524),
        secondary = Color(0xFFE7BCB4),
        background = Color(0xFF18110F),
        surface = Color(0xFF231917),
        onBackground = Color(0xFFEDE0DD),
        onSurface = Color(0xFFEDE0DD)
    )

    // 3. Mint Palette
    val MintLight = lightColorScheme(
        primary = Color(0xFF1D8A7F),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE0F4F2),
        secondary = Color(0xFF4D6260),
        background = Color(0xFFF4FAF9),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF191C1C),
        onSurface = Color(0xFF191C1C)
    )
    val MintDark = darkColorScheme(
        primary = Color(0xFF8CE1D6),
        onPrimary = Color(0xFF003732),
        primaryContainer = Color(0xFF005049),
        secondary = Color(0xFFB1CBC8),
        background = Color(0xFF0E1413),
        surface = Color(0xFF161C1B),
        onBackground = Color(0xFFE0E3E2),
        onSurface = Color(0xFFE0E3E2)
    )

    // 4. Sky Blue Palette
    val SkyBlueLight = lightColorScheme(
        primary = Color(0xFF1F7399),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE0F2FE),
        secondary = Color(0xFF4E616C),
        background = Color(0xFFF0F7FA),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF191C1E),
        onSurface = Color(0xFF191C1E)
    )
    val SkyBlueDark = darkColorScheme(
        primary = Color(0xFF8FD0E8),
        onPrimary = Color(0xFF00354A),
        primaryContainer = Color(0xFF004D6A),
        secondary = Color(0xFFB5CAD6),
        background = Color(0xFF0F1418),
        surface = Color(0xFF171C20),
        onBackground = Color(0xFFE1E2E4),
        onSurface = Color(0xFFE1E2E4)
    )

    // 5. Cream Palette
    val CreamLight = lightColorScheme(
        primary = Color(0xFF8A5D4D),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF8EFE9),
        secondary = Color(0xFF6C5D58),
        background = Color(0xFFFAF7F2),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1D1B19),
        onSurface = Color(0xFF1D1B19)
    )
    val CreamDark = darkColorScheme(
        primary = Color(0xFFE5C1B3),
        onPrimary = Color(0xFF522E22),
        primaryContainer = Color(0xFF6E4537),
        secondary = Color(0xFFD8C2BB),
        background = Color(0xFF161210),
        surface = Color(0xFF201B18),
        onBackground = Color(0xFFECE0DB),
        onSurface = Color(0xFFECE0DB)
    )

    // 6. Rose Palette
    val RoseLight = lightColorScheme(
        primary = Color(0xFFC7436E),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFECEF),
        secondary = Color(0xFF745660),
        background = Color(0xFFFFF5F7),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF201A1B),
        onSurface = Color(0xFF201A1B)
    )
    val RoseDark = darkColorScheme(
        primary = Color(0xFFFFB1C7),
        onPrimary = Color(0xFF5F1136),
        primaryContainer = Color(0xFF822A4E),
        secondary = Color(0xFFE3BDC7),
        background = Color(0xFF1C1113),
        surface = Color(0xFF271B1D),
        onBackground = Color(0xFFECE0E1),
        onSurface = Color(0xFFECE0E1)
    )

    // 7. Ocean Palette
    val OceanLight = lightColorScheme(
        primary = Color(0xFF007A9C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE2F6FC),
        secondary = Color(0xFF4C626A),
        background = Color(0xFFF2FAFBF),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF191C1D),
        onSurface = Color(0xFF191C1D)
    )
    val OceanDark = darkColorScheme(
        primary = Color(0xFF6ED2EC),
        onPrimary = Color(0xFF003644),
        primaryContainer = Color(0xFF004F62),
        secondary = Color(0xFFB3CBD3),
        background = Color(0xFF0C1417),
        surface = Color(0xFF141C1F),
        onBackground = Color(0xFFE0E3E4),
        onSurface = Color(0xFFE0E3E4)
    )

    fun getColorScheme(palette: ReaderThemePalette, isDark: Boolean): ColorScheme {
        return when (palette) {
            ReaderThemePalette.LAVENDER -> if (isDark) LavenderDark else LavenderLight
            ReaderThemePalette.PEACH -> if (isDark) PeachDark else PeachLight
            ReaderThemePalette.MINT -> if (isDark) MintDark else MintLight
            ReaderThemePalette.SKY_BLUE -> if (isDark) SkyBlueDark else SkyBlueLight
            ReaderThemePalette.CREAM -> if (isDark) CreamDark else CreamLight
            ReaderThemePalette.ROSE -> if (isDark) RoseDark else RoseLight
            ReaderThemePalette.OCEAN -> if (isDark) OceanDark else OceanLight
            else -> if (isDark) LavenderDark else LavenderLight // Fallback to Lavender representing premium theme
        }
    }

    // Special Reader view modes colors
    val EyeComfortBackground = Color(0xFFE8F5E9)      // Soft green back
    val EyeComfortText = Color(0xFF1B5E20)            // Forest dark green text
    val EyeComfortPrimary = Color(0xFF2E7D32)

    val WarmPaperBackground = Color(0xFFF4ECD8)       // Amber paper tone view
    val WarmPaperText = Color(0xFF3E2723)             // Sepia brown text
    val WarmPaperPrimary = Color(0xFF8D6E63)

    val NightBackground = Color(0xFF1B0F0B)           // Dark background with orange elements
    val NightText = Color(0xFFFF7043)                 // Reddish low-blue text
    val NightPrimary = Color(0xFFFF8A65)

    val DimBackground = Color(0xFF212121)             // Muted standard gray
    val DimText = Color(0xFFCFD8DC)                   // Muted teal text
    val DimPrimary = Color(0xFF78909C)

    val AmoleBackground = Color(0xFF000000)
    val AmoleText = Color(0xFFFFFFFF)
    val AmolePrimary = Color(0xFF9575CD)
}
