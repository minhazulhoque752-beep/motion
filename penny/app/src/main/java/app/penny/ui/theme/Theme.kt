package app.penny.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.penny.data.ThemeMode

/** Raw brand palette. Use [MaterialTheme.colorScheme] / [PennyTheme.colors] in UI code, never these directly. */
object Palette {
    val Forest = Color(0xFF2F5D46)
    val ForestDeep = Color(0xFF1F4231)
    val ForestSoft = Color(0xFFDCE6DD)
    val Sage = Color(0xFF8DB89E)
    val Ivory = Color(0xFFF4EFE3)
    val Paper = Color(0xFFF6F3EC)
    val PaperRaised = Color(0xFFFBF9F4)
    val Stone = Color(0xFFEDE9E0)
    val Hairline = Color(0xFFDDD8CC)
    val Charcoal = Color(0xFF1C1C1A)
    val Graphite = Color(0xFF5B5852)
    val Brick = Color(0xFF9C4434)
    val BrickSoft = Color(0xFFF3E1DC)

    val Ink = Color(0xFF121412)
    val InkRaised = Color(0xFF191C1A)
    val InkHigh = Color(0xFF222623)
    val InkLine = Color(0xFF30342F)
    val Bone = Color(0xFFECE8DF)
    val Ash = Color(0xFFA8A49B)
    val Clay = Color(0xFFE29A86)
}

private val LightScheme = lightColorScheme(
    primary = Palette.Forest,
    onPrimary = Palette.Ivory,
    primaryContainer = Palette.ForestSoft,
    onPrimaryContainer = Palette.ForestDeep,
    secondary = Palette.Graphite,
    onSecondary = Palette.Paper,
    secondaryContainer = Palette.Stone,
    onSecondaryContainer = Palette.Charcoal,
    tertiary = Palette.Forest,
    background = Palette.Paper,
    onBackground = Palette.Charcoal,
    surface = Palette.Paper,
    onSurface = Palette.Charcoal,
    surfaceVariant = Palette.Stone,
    onSurfaceVariant = Palette.Graphite,
    surfaceContainerLowest = Palette.PaperRaised,
    surfaceContainerLow = Palette.PaperRaised,
    surfaceContainer = Palette.PaperRaised,
    surfaceContainerHigh = Palette.Stone,
    surfaceContainerHighest = Palette.Stone,
    surfaceBright = Palette.PaperRaised,
    outline = Color(0xFFB9B3A6),
    outlineVariant = Palette.Hairline,
    error = Palette.Brick,
    onError = Palette.Ivory,
    errorContainer = Palette.BrickSoft,
    onErrorContainer = Color(0xFF5C2219),
    inverseSurface = Palette.Charcoal,
    inverseOnSurface = Palette.Paper,
    inversePrimary = Palette.Sage,
    scrim = Color(0x99000000),
)

private val DarkScheme = darkColorScheme(
    primary = Palette.Sage,
    onPrimary = Color(0xFF0F2418),
    primaryContainer = Color(0xFF23402F),
    onPrimaryContainer = Color(0xFFCFE3D5),
    secondary = Palette.Ash,
    onSecondary = Palette.Ink,
    secondaryContainer = Palette.InkHigh,
    onSecondaryContainer = Palette.Bone,
    tertiary = Palette.Sage,
    background = Palette.Ink,
    onBackground = Palette.Bone,
    surface = Palette.Ink,
    onSurface = Palette.Bone,
    surfaceVariant = Palette.InkHigh,
    onSurfaceVariant = Palette.Ash,
    surfaceContainerLowest = Palette.InkRaised,
    surfaceContainerLow = Palette.InkRaised,
    surfaceContainer = Palette.InkRaised,
    surfaceContainerHigh = Palette.InkHigh,
    surfaceContainerHighest = Palette.InkHigh,
    surfaceBright = Palette.InkHigh,
    outline = Color(0xFF4A4F49),
    outlineVariant = Palette.InkLine,
    error = Palette.Clay,
    onError = Color(0xFF3B0F07),
    errorContainer = Color(0xFF4A231B),
    onErrorContainer = Color(0xFFF6D5CC),
    inverseSurface = Palette.Bone,
    inverseOnSurface = Palette.Ink,
    inversePrimary = Palette.Forest,
    scrim = Color(0x99000000),
)

/** Semantic colours that Material's scheme doesn't cover. */
@Immutable
data class PennyColors(
    val positive: Color,
    val negative: Color,
    val hairline: Color,
    val raised: Color,
    val chartBar: Color,
    val chartBarMuted: Color,
    val chartGrid: Color,
    val sampleBadge: Color,
    val onSampleBadge: Color,
)

private val LightPenny = PennyColors(
    positive = Palette.Forest,
    negative = Palette.Brick,
    hairline = Palette.Hairline,
    raised = Palette.PaperRaised,
    chartBar = Palette.Forest,
    chartBarMuted = Color(0xFFC9D6CC),
    chartGrid = Color(0xFFE4DFD4),
    sampleBadge = Color(0xFFEDE3C8),
    onSampleBadge = Color(0xFF5E4B1C),
)

private val DarkPenny = PennyColors(
    positive = Palette.Sage,
    negative = Palette.Clay,
    hairline = Palette.InkLine,
    raised = Palette.InkRaised,
    chartBar = Palette.Sage,
    chartBarMuted = Color(0xFF35473C),
    chartGrid = Color(0xFF2A2E29),
    sampleBadge = Color(0xFF3D3522),
    onSampleBadge = Color(0xFFEAD9AE),
)

private val LocalPennyColors = staticCompositionLocalOf { LightPenny }

/** Restrained corner radii — crisp, not bubbly. */
private val PennyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

object PennyTheme {
    val colors: PennyColors
        @Composable get() = LocalPennyColors.current
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun PennyTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val scheme: ColorScheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(LocalPennyColors provides if (darkTheme) DarkPenny else LightPenny) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PennyTypography,
            shapes = PennyShapes,
            content = content,
        )
    }
}
