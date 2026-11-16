package app.penny.core

import kotlin.math.abs

/**
 * Money helpers. All amounts in Penny are stored as whole **paise** (1 rupee = 100 paise) in a [Long],
 * so arithmetic is exact and never suffers from floating point rounding.
 */
object Money {
    /** Largest single entry or goal target accepted: ₹1,00,00,000 (one crore). */
    const val MAX_AMOUNT_PAISE: Long = 1_00_00_000L * 100L

    private const val MAX_INTEGER_DIGITS = 8
    private val VALID_INPUT = Regex("""^(\d{1,9})(\.(\d{0,2}))?$""")

    /**
     * Parses user input such as `"1,250.5"`, `"₹ 300"` or `"0.75"` into paise.
     * Returns `null` for anything that is not a plain positive-or-zero decimal with at most 2 decimals.
     */
    fun parseToPaise(input: String): Long? {
        val cleaned = input.trim().replace("₹", "").replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        val normalized = if (cleaned.startsWith(".")) "0$cleaned" else cleaned
        val match = VALID_INPUT.matchEntire(normalized) ?: return null
        val rupees = match.groupValues[1].toLong()
        val fraction = match.groupValues[3].padEnd(2, '0')
        return rupees * 100 + fraction.toLong()
    }

    /**
     * Filters raw keyboard input for the amount field: digits and a single decimal point,
     * at most [MAX_INTEGER_DIGITS] rupee digits and 2 paise digits, no leading zeros.
     */
    fun sanitizeInput(raw: String): String {
        val sb = StringBuilder()
        var seenDot = false
        var decimals = 0
        var integerDigits = 0
        for (c in raw) {
            when {
                c.isDigit() && !seenDot -> {
                    if (integerDigits < MAX_INTEGER_DIGITS) {
                        sb.append(c); integerDigits++
                    }
                }
                c.isDigit() && seenDot -> {
                    if (decimals < 2) {
                        sb.append(c); decimals++
                    }
                }
                (c == '.') && !seenDot -> {
                    seenDot = true
                    if (integerDigits == 0) sb.append('0')
                    sb.append('.')
                }
                // A second decimal point means the rest is not part of this number (e.g. pasted "1.2.3").
                c == '.' -> break
            }
        }
        // Strip leading zeros ("007" -> "7", but keep "0" and "0.5").
        var result = sb.toString()
        while (result.length > 1 && result[0] == '0' && result[1] != '.') result = result.substring(1)
        return result
    }

    /** Converts paise to an editable string without grouping, e.g. 125050 -> "1250.50", 30000 -> "300". */
    fun toInputString(paise: Long): String {
        val rupees = paise / 100
        val p = paise % 100
        return if (p == 0L) rupees.toString() else "$rupees.${p.toString().padStart(2, '0')}"
    }

    /**
     * Formats paise as Indian Rupees with Indian digit grouping: `₹1,00,000.00`.
     * @param decimals show paise (2 decimals). When false the value is rounded half-up to whole rupees.
     * @param signed prefix positive values with "+". Negative values always get a minus sign.
     */
    fun formatInr(paise: Long, decimals: Boolean = true, signed: Boolean = false): String {
        val negative = paise < 0
        val absolute = abs(paise)
        val body = if (decimals) {
            "${groupIndian(absolute / 100)}.${(absolute % 100).toString().padStart(2, '0')}"
        } else {
            groupIndian((absolute + 50) / 100)
        }
        val sign = when {
            negative -> "−"
            signed && paise > 0 -> "+"
            else -> ""
        }
        return "$sign₹$body"
    }

    /** Short form for chart axes and tight spaces: ₹950, ₹12.5K, ₹3.2L, ₹1.1Cr. */
    fun formatCompact(paise: Long): String {
        val negative = paise < 0
        val rupees = abs(paise) / 100.0
        val body = when {
            rupees >= 1_00_00_000 -> trim1(rupees / 1_00_00_000) + "Cr"
            rupees >= 1_00_000 -> trim1(rupees / 1_00_000) + "L"
            rupees >= 1_000 -> trim1(rupees / 1_000) + "K"
            else -> Math.round(rupees).toString()
        }
        return (if (negative) "−₹" else "₹") + body
    }

    private fun trim1(v: Double): String {
        val rounded = Math.round(v * 10) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    /** Groups digits the Indian way: last three digits, then pairs. 1234567 -> "12,34,567". */
    fun groupIndian(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val lastThree = s.takeLast(3)
        var rest = s.dropLast(3)
        val sb = StringBuilder()
        while (rest.length > 2) {
            sb.insert(0, "," + rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        return "$rest$sb,$lastThree"
    }

    /** Plain machine-readable decimal for CSV: 125050 -> "1250.50", -500 -> "-5.00". */
    fun toPlainDecimal(paise: Long): String {
        val negative = paise < 0
        val a = abs(paise)
        return (if (negative) "-" else "") + "${a / 100}.${(a % 100).toString().padStart(2, '0')}"
    }
}
