package app.penny.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.penny.R

/** Inter (SIL Open Font License) — a neutral, Swiss-style grotesque with tabular figures for money. */
val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** Tabular, lining figures so amounts align in columns. */
private const val NUMERIC = "tnum, lnum"

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

val PennyTypography = Typography(
    displayLarge = style(46, FontWeight.Light, 52, -0.03).copy(fontFeatureSettings = NUMERIC),
    displayMedium = style(36, FontWeight.Light, 42, -0.025).copy(fontFeatureSettings = NUMERIC),
    displaySmall = style(30, FontWeight.Light, 36, -0.02).copy(fontFeatureSettings = NUMERIC),
    headlineLarge = style(28, FontWeight.Medium, 34, -0.02),
    headlineMedium = style(24, FontWeight.Medium, 30, -0.015),
    headlineSmall = style(22, FontWeight.Medium, 28, -0.01),
    titleLarge = style(19, FontWeight.Medium, 26, -0.01),
    titleMedium = style(16, FontWeight.Medium, 22, -0.005),
    titleSmall = style(14, FontWeight.Medium, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 16, 0.005),
    labelLarge = style(14, FontWeight.Medium, 20, 0.005),
    labelMedium = style(12, FontWeight.Medium, 16, 0.01),
    labelSmall = style(11, FontWeight.Medium, 14, 0.08),
)

/** Text style helpers used across screens. */
object PennyText {
    /** Small uppercase label in the Swiss editorial tradition. */
    val overline: TextStyle get() = PennyTypography.labelSmall

    /** Amount style for list rows and stat tiles. */
    val amount: TextStyle get() = style(15, FontWeight.Medium, 20).copy(fontFeatureSettings = NUMERIC)
    val amountLarge: TextStyle get() = style(22, FontWeight.Normal, 28, -0.01).copy(fontFeatureSettings = NUMERIC)
}
