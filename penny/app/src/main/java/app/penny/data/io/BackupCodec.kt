package app.penny.data.io

import app.penny.core.Money
import app.penny.domain.Category
import app.penny.domain.Entry
import app.penny.domain.EntryType
import app.penny.domain.EntryValidator
import app.penny.domain.Goal
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

class BackupFormatException(message: String) : Exception(message)

data class BackupData(
    val exportedAt: Long,
    val goals: List<Goal>,
    val entries: List<Entry>,
)

/**
 * Penny's backup file: a self-describing JSON document. Amounts are stored in paise and dates as ISO-8601
 * strings, so a backup is readable, diff-able and restorable on any device.
 */
object BackupCodec {
    const val FORMAT = "penny-backup"
    const val VERSION = 1

    fun encode(entries: List<Entry>, goals: List<Goal>, exportedAt: Long): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", exportedAt)
        root.put("currency", "INR")
        root.put("goals", JSONArray().apply {
            goals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("title", g.title)
                    put("targetPaise", g.targetPaise)
                    put("targetDate", g.targetDate?.toString() ?: JSONObject.NULL)
                    put("createdAt", g.createdAt)
                })
            }
        })
        root.put("entries", JSONArray().apply {
            entries.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("amountPaise", e.amountPaise)
                    put("type", e.type.name)
                    put("category", e.category.name)
                    put("note", e.note)
                    put("date", e.date.toString())
                    put("goalId", e.goalId ?: JSONObject.NULL)
                    put("createdAt", e.createdAt)
                    put("updatedAt", e.updatedAt)
                })
            }
        })
        return root.toString(2)
    }

    /** Parses and validates a backup. Throws [BackupFormatException] with a user-readable message on any problem. */
    fun decode(text: String): BackupData {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw BackupFormatException("This file isn't a Penny backup.")
        }
        if (root.optString("format") != FORMAT) throw BackupFormatException("This file isn't a Penny backup.")
        val version = root.optInt("version", -1)
        if (version < 1 || version > VERSION) throw BackupFormatException("This backup was made by a newer version of Penny.")

        try {
            val goalsJson = root.getJSONArray("goals")
            val goals = (0 until goalsJson.length()).map { i ->
                val o = goalsJson.getJSONObject(i)
                val title = o.getString("title").trim()
                val target = o.getLong("targetPaise")
                if (EntryValidator.validateGoal(title, target) != null) throw BackupFormatException("A goal in this backup is invalid.")
                Goal(
                    id = o.getLong("id"),
                    title = title,
                    targetPaise = target,
                    targetDate = if (o.isNull("targetDate")) null else LocalDate.parse(o.getString("targetDate")),
                    createdAt = o.optLong("createdAt", 0L),
                )
            }
            if (goals.map { it.id }.toSet().size != goals.size) throw BackupFormatException("This backup contains duplicate goals.")
            val goalIds = goals.map { it.id }.toSet()

            val entriesJson = root.getJSONArray("entries")
            val entries = (0 until entriesJson.length()).map { i ->
                val o = entriesJson.getJSONObject(i)
                val type = runCatching { EntryType.valueOf(o.getString("type")) }.getOrNull()
                    ?: throw BackupFormatException("An entry in this backup has an unknown type.")
                val goalId = if (o.isNull("goalId")) null else o.getLong("goalId").takeIf { it in goalIds }
                Entry(
                    id = o.getLong("id"),
                    amountPaise = o.getLong("amountPaise"),
                    type = type,
                    category = Category.fromName(o.optString("category")),
                    note = o.optString("note", "").take(EntryValidator.MAX_NOTE_LENGTH),
                    date = LocalDate.parse(o.getString("date")),
                    goalId = goalId,
                    createdAt = o.optLong("createdAt", 0L),
                    updatedAt = o.optLong("updatedAt", 0L),
                )
            }
            if (entries.map { it.id }.toSet().size != entries.size) throw BackupFormatException("This backup contains duplicate entries.")
            if (entries.any { it.amountPaise <= 0 || it.amountPaise > Money.MAX_AMOUNT_PAISE }) {
                throw BackupFormatException("This backup contains an invalid amount.")
            }
            if (!EntryValidator.isConsistent(entries)) {
                throw BackupFormatException("This backup's balances don't add up, so it can't be restored safely.")
            }
            return BackupData(root.optLong("exportedAt", 0L), goals, entries)
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: Exception) {
            // JSONException, DateTimeParseException, etc.
            throw BackupFormatException("This backup file is damaged or incomplete.")
        }
    }
}
