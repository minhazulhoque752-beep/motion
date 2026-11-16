package app.penny.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Penny's own line-icon set: 24dp grid, 1.6dp stroke, round caps and joins.
 * Drawn once and reused so every glyph shares the same weight and proportions.
 */
object PennyIcons {
    val Home by lazy { line("home", "M4 10.2 12 4l8 6.2V19a1 1 0 0 1-1 1h-4.5v-5.5h-5V20H5a1 1 0 0 1-1-1z") }
    val History by lazy { line("history", "M3.5 12a8.5 8.5 0 1 0 2.5-6", "M3.5 4.5V8H7", "M12 7.5V12l3 2") }
    val Goals by lazy {
        line("goals", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M12 7.5a4.5 4.5 0 1 0 0 9a4.5 4.5 0 1 0 0-9z", "M12 11.4v1.2")
    }
    val Insights by lazy { line("insights", "M4 20h16", "M6.5 16v-4", "M11 16V8", "M15.5 16v-6", "M20 16V5") }
    val Plus by lazy { line("plus", "M12 5v14", "M5 12h14") }
    val Minus by lazy { line("minus", "M5 12h14") }
    val Search by lazy { line("search", "M10.8 4a6.8 6.8 0 1 0 0 13.6a6.8 6.8 0 1 0 0-13.6z", "M20 20l-4.4-4.4") }
    val Back by lazy { line("back", "M19 12H5", "M11 6l-6 6 6 6") }
    val Close by lazy { line("close", "M6 6l12 12", "M18 6 6 18") }
    val Edit by lazy { line("edit", "M4 20h4L18.6 9.4a2.8 2.8 0 0 0-4-4L4 16z", "M13.2 6.8l4 4") }
    val Delete by lazy {
        line("delete", "M4 7h16", "M10 11v6", "M14 11v6", "M6 7l.9 12.1a2 2 0 0 0 2 1.9h6.2a2 2 0 0 0 2-1.9L18 7", "M9 7V4.5h6V7")
    }
    val Calendar by lazy {
        line("calendar", "M4.5 6.5a1.5 1.5 0 0 1 1.5-1.5h12a1.5 1.5 0 0 1 1.5 1.5V19a1.5 1.5 0 0 1-1.5 1.5H6A1.5 1.5 0 0 1 4.5 19z", "M4.5 10h15", "M8.5 3v4", "M15.5 3v4")
    }
    val ChevronRight by lazy { line("chevron_right", "M9.5 6l6 6-6 6") }
    val ChevronLeft by lazy { line("chevron_left", "M14.5 6l-6 6 6 6") }
    val Settings by lazy {
        line(
            "settings", "M4 7h9", "M19 7h1", "M4 17h3", "M13 17h7",
            "M16 4.8a2.2 2.2 0 1 0 0 4.4a2.2 2.2 0 1 0 0-4.4z", "M10 14.8a2.2 2.2 0 1 0 0 4.4a2.2 2.2 0 1 0 0-4.4z",
        )
    }
    val Bell by lazy { line("bell", "M6.5 16.5v-5.5a5.5 5.5 0 0 1 11 0v5.5l1.5 2h-14z", "M10 21h4") }
    val Export by lazy { line("export", "M12 4v11", "M7.5 10.5 12 15l4.5-4.5", "M5 20h14") }
    val Backup by lazy { line("backup", "M12 15V4", "M7.5 8.5 12 4l4.5 4.5", "M5 20h14") }
    val Restore by lazy { line("restore", "M20.5 12a8.5 8.5 0 1 1-2.5-6", "M20.5 4.5V8H17", "M12 8v4.5") }
    val Shield by lazy { line("shield", "M12 3.2l7 2.8v5.2c0 4.4-3 8.2-7 9.6-4-1.4-7-5.2-7-9.6V6z", "M9 12l2 2 4-4") }
    val Contrast by lazy { line("contrast", "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0-17z", "M12 3.5v17", "M12 8h4.5", "M12 12h6.5", "M12 16h4.5") }
    val Check by lazy { line("check", "M5 12.5l4.5 4.5L19 7.5") }
    val User by lazy { line("user", "M12 4a4 4 0 1 0 0 8a4 4 0 1 0 0-8z", "M4.5 20.5a7.5 7.5 0 0 1 15 0") }
    val Info by lazy { line("info", "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0-17z", "M12 11v5.5", "M12 7.8v.2") }
    val Target by lazy { line("target", "M12 4.5a7.5 7.5 0 1 0 7.5 7.5", "M12 9a3 3 0 1 0 3 3", "M12 12l7-7", "M16 5h3v3") }
    val Clear by lazy { line("clear", "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0-17z", "M9.2 9.2l5.6 5.6", "M14.8 9.2l-5.6 5.6") }
    val Note by lazy { line("note", "M6 3.5h8.5L18 7v13.5H6z", "M9 11h6", "M9 15h6") }
    val Wallet by lazy {
        line("wallet", "M4 7.5A2.5 2.5 0 0 1 6.5 5H18v3", "M4 7.5V18a2 2 0 0 0 2 2h13a1 1 0 0 0 1-1v-9a1 1 0 0 0-1-1H6.5A2.5 2.5 0 0 1 4 7.5", "M16 14.5h.5")
    }
    val Message by lazy { line("message", "M4.5 5.5h15v10h-9l-4 3.5v-3.5h-2z") }
    val Clock by lazy { line("clock", "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0-17z", "M12 7.5V12l3 2") }
    val Skip by lazy { line("skip", "M5 6.5v11l8-5.5z", "M17 6.5v11") }

    private fun line(name: String, vararg paths: String, stroke: Float = 1.6f): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { d ->
                    addPath(
                        pathData = addPathNodes(d),
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = stroke,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
            .build()
}
