package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeSetting

private val AegeanNightColorScheme = darkColorScheme(
    primary = CyanZephyr,
    onPrimary = SkyAbyss,
    primaryContainer = Color(0xFF0E3A5D),
    onPrimaryContainer = CyanGlow,
    secondary = AmberSol,
    onSecondary = SkyAbyss,
    secondaryContainer = Color(0xFF423006),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = EmeraldBreeze,
    onTertiary = SkyAbyss,
    background = SkyAbyss,
    onBackground = TextPrimaryDark,
    surface = SkyMidnight,
    onSurface = TextPrimaryDark,
    surfaceVariant = SkySlateSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = SkyBorderSubtle,
    error = CoralWarmth
)

private val MediterraneanDawnColorScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF1B1A3A),
    primaryContainer = Color(0xFF5A3A10),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = Color(0xFFFB7185),
    onSecondary = Color(0xFF1B1A3A),
    secondaryContainer = Color(0xFF4C1D24),
    onSecondaryContainer = Color(0xFFFECDD3),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF1B1A3A),
    background = Color(0xFF131126),
    onBackground = Color(0xFFFFF7ED),
    surface = Color(0xFF1C1938),
    onSurface = Color(0xFFFFF7ED),
    surfaceVariant = Color(0xFF28234D),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF4A3B69),
    error = Color(0xFFF43F5E)
)

private val CycladicCobaltColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF031633),
    primaryContainer = Color(0xFF073763),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF031633),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF031633),
    background = Color(0xFF021226),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF061E3D),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF0C2C57),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1D4E89),
    error = Color(0xFFFB7185)
)

private val OnyxMinimalColorScheme = darkColorScheme(
    primary = Color(0xFFCBD5E1),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF171717),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF262626),
    error = Color(0xFFEF4444)
)

@Composable
fun MyApplicationTheme(
    themeSetting: AppThemeSetting = AppThemeSetting.AEGEAN_NIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeSetting) {
        AppThemeSetting.AEGEAN_NIGHT -> AegeanNightColorScheme
        AppThemeSetting.MEDITERRANEAN_DAWN -> MediterraneanDawnColorScheme
        AppThemeSetting.CYCLADIC_COBALT -> CycladicCobaltColorScheme
        AppThemeSetting.ONYX_MINIMAL -> OnyxMinimalColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
