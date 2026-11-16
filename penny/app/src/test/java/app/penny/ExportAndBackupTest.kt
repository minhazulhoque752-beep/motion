package app.penny

import app.penny.data.io.BackupCodec
import app.penny.data.io.BackupFormatException
import app.penny.data.io.CsvExporter
import app.penny.domain.Category
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExportAndBackupTest {
    private val today = d(2026, 11, 16)

    @Test fun csvHasHeaderRowsNewestFirstAndPlainAmounts() {
        val g = goal(1, "New Phone", 30_000)
        val older = deposit(1_250, d(2026, 11, 1), goalId = 1, category = Category.SHOPPING, note = "Phone, part 1", paise = 50)
        val newer = withdrawal(200, today, note = "Repair")
        val csv = CsvExporter.export(listOf(older, newer), listOf(g))
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("Date,Type,Amount (INR),Signed amount (INR),Category,Goal,Note", lines[0])
        assertEquals("2026-11-16,Withdrawal,200.00,-200.00,General,,Repair", lines[1])
        assertEquals("2026-11-01,Saving,1250.50,1250.50,Shopping,New Phone,\"Phone, part 1\"", lines[2])
        assertEquals(3, lines.size)
    }

    @Test fun csvEscapesQuotesNewlinesAndFormulas() {
        assertEquals("\"He said \"\"hi\"\"\"", CsvExporter.escape("He said \"hi\""))
        assertEquals("\"a\nb\"", CsvExporter.escape("a\nb"))
        assertEquals("'=SUM(A1:A2)", CsvExporter.escape("=SUM(A1:A2)"))
        assertEquals("'@cmd", CsvExporter.escape("@cmd"))
        assertEquals("-200.00", CsvExporter.escape("-200.00")) // numbers are untouched
        assertEquals("plain", CsvExporter.escape("plain"))
    }

    @Test fun backupRoundTripsExactly() {
        val goals = listOf(goal(1, "Emergency Fund", 50_000, d(2027, 6, 30)), goal(2, "Travel", 20_000))
        val entries = listOf(
            deposit(10_000, d(2026, 10, 1), goalId = 1, category = Category.EMERGENCY, note = "Bonus ₹ \"quoted\""),
            deposit(2_000, d(2026, 11, 2), goalId = 2, category = Category.TRAVEL),
            withdrawal(500, today, goalId = 2),
            deposit(1, today, paise = 99),
        )
        val json = BackupCodec.encode(entries, goals, exportedAt = 1_700_000_000_000)
        val restored = BackupCodec.decode(json)
        assertEquals(goals, restored.goals)
        assertEquals(entries, restored.entries)
        assertEquals(1_700_000_000_000, restored.exportedAt)
    }

    @Test fun backupRejectsForeignFiles() {
        expectFailure("not json at all")
        expectFailure("{}")
        expectFailure("""{"format":"something-else","version":1,"goals":[],"entries":[]}""")
        expectFailure("""{"format":"penny-backup","version":99,"goals":[],"entries":[]}""")
        expectFailure("""{"format":"penny-backup","version":1,"goals":[]}""")
    }

    @Test fun backupRejectsInvalidData() {
        val base = JSONObject(BackupCodec.encode(listOf(deposit(100, today)), emptyList(), 0))
        base.getJSONArray("entries").getJSONObject(0).put("amountPaise", -5)
        expectFailure(base.toString())

        val badDate = JSONObject(BackupCodec.encode(listOf(deposit(100, today)), emptyList(), 0))
        badDate.getJSONArray("entries").getJSONObject(0).put("date", "2026-13-45")
        expectFailure(badDate.toString())

        // Withdrawals larger than savings would produce a negative balance.
        expectFailure(BackupCodec.encode(listOf(deposit(100, today), withdrawal(200, today)), emptyList(), 0))
    }

    @Test fun backupDropsReferencesToMissingGoals() {
        val json = BackupCodec.encode(listOf(deposit(100, today, goalId = 42)), emptyList(), 0)
        val restored = BackupCodec.decode(json)
        assertNull(restored.entries.single().goalId)
    }

    @Test fun backupToleratesUnknownCategory() {
        val obj = JSONObject(BackupCodec.encode(listOf(deposit(100, today)), emptyList(), 0))
        obj.getJSONArray("entries").getJSONObject(0).put("category", "FUTURE_CATEGORY")
        assertEquals(Category.OTHER, BackupCodec.decode(obj.toString()).entries.single().category)
    }

    private fun expectFailure(text: String) {
        try {
            BackupCodec.decode(text)
            fail("Expected BackupFormatException for: ${text.take(60)}")
        } catch (e: BackupFormatException) {
            assertTrue(e.message!!.isNotBlank())
        }
    }
}
