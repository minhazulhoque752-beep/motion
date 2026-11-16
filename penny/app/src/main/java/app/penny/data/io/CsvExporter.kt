package app.penny.data.io

import app.penny.core.Money
import app.penny.domain.Entry
import app.penny.domain.EntryType
import app.penny.domain.Goal
import app.penny.domain.SavingsCalculator

/** Builds a spreadsheet-friendly CSV (RFC 4180) of the full transaction history, newest first. */
object CsvExporter {
    val HEADER = listOf("Date", "Type", "Amount (INR)", "Signed amount (INR)", "Category", "Goal", "Note")

    fun export(entries: List<Entry>, goals: List<Goal>): String {
        val titles = goals.associate { it.id to it.title }
        val sb = StringBuilder()
        sb.append(HEADER.joinToString(",") { escape(it) }).append("\r\n")
        for (e in SavingsCalculator.sortNewestFirst(entries)) {
            val row = listOf(
                e.date.toString(),
                if (e.type == EntryType.DEPOSIT) "Saving" else "Withdrawal",
                Money.toPlainDecimal(e.amountPaise),
                Money.toPlainDecimal(e.signedPaise),
                e.category.label,
                e.goalId?.let { titles[it] }.orEmpty(),
                e.note,
            )
            sb.append(row.joinToString(",") { escape(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /**
     * Quotes fields containing separators/quotes/newlines, and neutralises spreadsheet formula injection
     * (text fields starting with = + - @ are prefixed with an apostrophe). Numeric columns never hit this branch
     * because only free-text fields can start with those characters.
     */
    fun escape(field: String): String {
        val looksNumeric = field.matches(Regex("""^-?\d+(\.\d+)?$"""))
        val safe = if (!looksNumeric && field.isNotEmpty() && field[0] in "=+-@\t\r") "'$field" else field
        val needsQuotes = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
