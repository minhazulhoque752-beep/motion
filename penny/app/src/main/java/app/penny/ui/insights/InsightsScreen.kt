package app.penny.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.penny.core.Money
import app.penny.core.Periods
import app.penny.domain.MonthPoint
import app.penny.ui.common.EmptyState
import app.penny.ui.common.ErrorState
import app.penny.ui.common.LoadingState
import app.penny.ui.common.MoneyText
import app.penny.ui.common.Overline
import app.penny.ui.common.PennyCard
import app.penny.ui.common.ProgressLine
import app.penny.ui.common.ScreenHeader
import app.penny.ui.common.ScreenPadding
import app.penny.ui.common.Segmented
import app.penny.ui.common.pennyViewModel
import app.penny.ui.home.BalanceText
import app.penny.ui.icons.PennyIcons
import app.penny.ui.theme.PennyText
import app.penny.ui.theme.PennyTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(onAdd: () -> Unit) {
    val vm = pennyViewModel { InsightsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Insights")
        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(state.error!!)
            !state.hasEntries -> EmptyState(
                PennyIcons.Insights,
                "Insights need a little history",
                "Once you record savings, Penny will chart your months, compare them and find your best one.",
                actionLabel = "Add savings",
                onAction = onAdd,
            )
            else -> InsightsContent(state, onMonths = vm::setMonths)
        }
    }
}

@Composable
internal fun InsightsContent(state: InsightsUiState, onMonths: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 32.dp)) {
        item(key = "total") {
            Column(Modifier.padding(top = 8.dp, bottom = 20.dp)) {
                Overline("Total savings")
                Spacer(Modifier.height(6.dp))
                BalanceText(state.totalPaise, MaterialTheme.typography.displayMedium)
            }
        }

        item(key = "stats") {
            PennyCard(Modifier.fillMaxWidth()) {
                Row {
                    StatCell("This month", state.monthPaise, Modifier.weight(1f))
                    StatCell("This week", state.weekPaise, Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))
                Row {
                    StatCell("Today", state.todayPaise, Modifier.weight(1f))
                    StatCell("Daily average", state.dailyAveragePaise, Modifier.weight(1f), caption = "this month")
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item(key = "trend") {
            TrendCard(state, onMonths)
            Spacer(Modifier.height(16.dp))
        }

        state.comparison?.let { cmp ->
            item(key = "comparison") {
                PennyCard(Modifier.fillMaxWidth()) {
                    Overline("Monthly comparison")
                    Spacer(Modifier.height(14.dp))
                    val maxVal = maxOf(abs(cmp.current.netPaise), abs(cmp.previous.netPaise), 1L)
                    ComparisonRow(Periods.formatMonth(cmp.current.month), cmp.current.netPaise, maxVal, highlight = true)
                    Spacer(Modifier.height(12.dp))
                    ComparisonRow(Periods.formatMonth(cmp.previous.month), cmp.previous.netPaise, maxVal, highlight = false)
                    Spacer(Modifier.height(14.dp))
                    val change = cmp.changePercent
                    Text(
                        when {
                            change == null && cmp.current.netPaise > 0 -> "A fresh start — nothing saved in ${Periods.formatMonthShort(cmp.previous.month)}."
                            change == null -> "Nothing to compare yet."
                            change >= 0 -> "Up ${change.roundToInt()}% on ${Periods.formatMonthShort(cmp.previous.month)}."
                            else -> "Down ${(-change).roundToInt()}% on ${Periods.formatMonthShort(cmp.previous.month)}."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if ((change ?: 0.0) < 0) PennyTheme.colors.negative else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        state.best?.let { best ->
            item(key = "best") {
                PennyCard(Modifier.fillMaxWidth()) {
                    Overline("Best savings month")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Periods.formatMonth(best.month), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        MoneyText(best.netPaise, style = PennyText.amountLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Saved ${Money.formatInr(best.depositsPaise)}" +
                            if (best.withdrawalsPaise > 0) ", withdrew ${Money.formatInr(best.withdrawalsPaise)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        if (state.categories.isNotEmpty()) {
            item(key = "categories") {
                PennyCard(Modifier.fillMaxWidth()) {
                    Overline("Savings by category")
                    Spacer(Modifier.height(6.dp))
                    state.categories.forEach { share ->
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(share.category.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(
                                "${Money.formatInr(share.depositsPaise)}  ·  ${(share.fraction * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        ProgressLine(share.fraction, height = 3.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, paise: Long, modifier: Modifier = Modifier, caption: String? = null) {
    Column(modifier) {
        Overline(label)
        Spacer(Modifier.height(4.dp))
        MoneyText(paise, style = PennyText.amount, color = if (paise < 0) PennyTheme.colors.negative else MaterialTheme.colorScheme.onSurface)
        if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ComparisonRow(label: String, paise: Long, maxVal: Long, highlight: Boolean) {
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            MoneyText(paise, style = PennyText.amount, color = if (paise < 0) PennyTheme.colors.negative else MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        ProgressLine(
            (abs(paise).toDouble() / maxVal).toFloat(),
            color = when {
                paise < 0 -> PennyTheme.colors.negative
                highlight -> PennyTheme.colors.chartBar
                else -> PennyTheme.colors.chartBarMuted
            },
            height = 6.dp,
        )
    }
}

@Composable
private fun TrendCard(state: InsightsUiState, onMonths: (Int) -> Unit) {
    val series = state.series
    var selected by rememberSaveable(state.months) { mutableIntStateOf(series.lastIndex) }
    val sel = series.getOrNull(selected) ?: series.lastOrNull()
    PennyCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Savings trend", Modifier.weight(1f))
            Segmented(
                options = listOf(6, 12),
                selected = state.months,
                label = { "${it}M" },
                onSelect = onMonths,
                modifier = Modifier.width(120.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        if (sel != null) {
            Text(Periods.formatMonth(sel.month), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(
                sel.netPaise,
                style = PennyText.amountLarge,
                color = if (sel.netPaise < 0) PennyTheme.colors.negative else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(16.dp))
        MonthlyBarChart(
            points = series,
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            series.forEachIndexed { i, p ->
                val label = Periods.formatMonthShort(p.month)
                Text(
                    if (series.size > 6 && i % 2 == 1 && i != selected) "" else label.take(if (series.size > 6) 1 else 3),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Net savings per month (saved − withdrawn). Tap a bar for details.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Monthly net savings as bars. Positive months rise from the baseline; months with net withdrawals drop below it.
 */
@Composable
fun MonthlyBarChart(
    points: List<MonthPoint>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PennyTheme.colors
    val negative = colors.negative
    val description = remember(points) {
        points.joinToString("; ") { "${Periods.formatMonth(it.month)}: ${Money.formatInr(it.netPaise)}" }
    }
    Canvas(
        modifier
            .semantics { contentDescription = "Savings trend. $description" }
            .pointerInput(points.size) {
                detectTapGestures { offset ->
                    if (points.isNotEmpty()) {
                        val slot = size.width.toFloat() / points.size
                        onSelect((offset.x / slot).toInt().coerceIn(0, points.lastIndex))
                    }
                }
            },
    ) {
        if (points.isEmpty()) return@Canvas
        val maxPos = points.maxOf { it.netPaise }.coerceAtLeast(0)
        val maxNeg = (-points.minOf { it.netPaise }).coerceAtLeast(0)
        val range = (maxPos + maxNeg).coerceAtLeast(1)
        val baseline = size.height * (maxPos.toFloat() / range).let { if (maxNeg == 0L) 1f else it }
        val slot = size.width / points.size
        val barWidth = (slot * 0.56f).coerceAtMost(28.dp.toPx())
        val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        // Gridlines at top and middle, plus the baseline.
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        drawLine(colors.chartGrid, Offset(0f, 0.5f), Offset(size.width, 0.5f), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        drawLine(colors.chartGrid, Offset(0f, baseline / 2), Offset(size.width, baseline / 2), strokeWidth = 1.dp.toPx(), pathEffect = dash)

        points.forEachIndexed { i, p ->
            val v = p.netPaise
            val h = abs(v).toFloat() / range * size.height
            val x = slot * i + (slot - barWidth) / 2
            val isSel = i == selected
            val color: Color = when {
                v < 0 -> if (isSel) negative else negative.copy(alpha = 0.45f)
                isSel -> colors.chartBar
                else -> colors.chartBarMuted
            }
            if (v != 0L) {
                val minH = 2.dp.toPx()
                val barH = h.coerceAtLeast(minH)
                val top = if (v > 0) baseline - barH else baseline
                drawRoundRect(color, topLeft = Offset(x, top), size = Size(barWidth, barH), cornerRadius = radius)
            } else {
                // Empty month: a small tick on the baseline keeps the rhythm of the axis.
                drawRoundRect(colors.chartGrid, topLeft = Offset(x, baseline - 1.dp.toPx()), size = Size(barWidth, 2.dp.toPx()), cornerRadius = radius)
            }
        }
        drawLine(colors.hairline, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.dp.toPx())
    }
}
