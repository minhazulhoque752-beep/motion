package app.penny.data

import androidx.room.withTransaction
import app.penny.data.db.PennyDatabase
import app.penny.data.db.toDomain
import app.penny.data.db.toEntity
import app.penny.data.io.BackupData
import app.penny.domain.Entry
import app.penny.domain.EntryDraft
import app.penny.domain.EntryValidator
import app.penny.domain.Goal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

/**
 * Single source of truth for the user's ledger. Every write is validated *inside* a database transaction against
 * the current stored data, so concurrent or repeated taps can never produce an inconsistent ledger.
 */
class SavingsRepository(
    private val db: PennyDatabase,
    private val clock: Clock,
) {
    private val entryDao = db.entryDao()
    private val goalDao = db.goalDao()

    val entries: Flow<List<Entry>> = entryDao.observeAll().map { list -> list.map { it.toDomain() } }
    val goals: Flow<List<Goal>> = goalDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getEntry(id: Long): Entry? = entryDao.getById(id)?.toDomain()

    /** Inserts or updates an entry. Returns an error message, or null on success. */
    suspend fun saveEntry(draft: EntryDraft, editingId: Long?): String? = db.withTransaction {
        val existing = entryDao.getAll().map { it.toDomain() }
        val goalIds = goalDao.getAll().map { it.id }.toSet()
        val today = LocalDate.now(clock)
        EntryValidator.validate(draft, existing, editingId, today, goalIds)?.let { return@withTransaction it }

        val now = clock.millis()
        val amount = requireNotNull(draft.amountPaise)
        if (editingId == null) {
            entryDao.insert(
                Entry(
                    id = 0, amountPaise = amount, type = draft.type, category = draft.category,
                    note = draft.note.trim(), date = draft.date, goalId = draft.goalId, createdAt = now, updatedAt = now,
                ).toEntity(),
            )
        } else {
            val old = existing.firstOrNull { it.id == editingId } ?: return@withTransaction "This entry no longer exists"
            entryDao.update(
                old.copy(
                    amountPaise = amount, type = draft.type, category = draft.category, note = draft.note.trim(),
                    date = draft.date, goalId = draft.goalId, updatedAt = now,
                ).toEntity(),
            )
        }
        null
    }

    /** Deletes an entry. Returns the deleted entry (for undo) or an error message. */
    suspend fun deleteEntry(id: Long): DeleteResult = db.withTransaction {
        val existing = entryDao.getAll().map { it.toDomain() }
        val target = existing.firstOrNull { it.id == id } ?: return@withTransaction DeleteResult.Failed("This entry no longer exists")
        EntryValidator.validateDelete(id, existing)?.let { return@withTransaction DeleteResult.Failed(it) }
        entryDao.deleteById(id)
        DeleteResult.Deleted(target)
    }

    /** Re-inserts a just-deleted entry (Undo). Validated like any other change. */
    suspend fun restoreEntry(entry: Entry): String? = db.withTransaction {
        val existing = entryDao.getAll().map { it.toDomain() }
        val goalIds = goalDao.getAll().map { it.id }.toSet()
        val restored = if (entry.goalId != null && entry.goalId !in goalIds) entry.copy(goalId = null) else entry
        val all = existing + restored
        if (!EntryValidator.isConsistent(all)) return@withTransaction "Couldn't undo — balances have changed"
        entryDao.insert(restored.toEntity())
        null
    }

    suspend fun saveGoal(id: Long?, title: String, targetPaise: Long?, targetDate: LocalDate?): String? {
        EntryValidator.validateGoal(title, targetPaise)?.let { return it }
        val target = requireNotNull(targetPaise)
        return db.withTransaction {
            if (id == null) {
                goalDao.insert(
                    Goal(0, title.trim(), target, targetDate, clock.millis()).toEntity(),
                )
                null
            } else {
                val old = goalDao.getAll().firstOrNull { it.id == id } ?: return@withTransaction "This goal no longer exists"
                goalDao.update(old.copy(title = title.trim(), targetPaise = target, targetDate = targetDate?.toEpochDay()))
                null
            }
        }
    }

    /** Deletes a goal. Its entries are kept and become unassigned (foreign key ON DELETE SET NULL). */
    suspend fun deleteGoal(id: Long) {
        goalDao.deleteById(id)
    }

    suspend fun hasEntryOn(date: LocalDate): Boolean = entryDao.countOnDay(date.toEpochDay()) > 0

    suspend fun snapshot(): Pair<List<Entry>, List<Goal>> = db.withTransaction {
        entryDao.getAll().map { it.toDomain() } to goalDao.getAll().map { it.toDomain() }
    }

    /** Replaces everything with the contents of a validated backup, atomically. */
    suspend fun replaceAll(data: BackupData) {
        db.withTransaction {
            entryDao.deleteAll()
            goalDao.deleteAll()
            goalDao.insertAll(data.goals.map { it.toEntity() })
            entryDao.insertAll(data.entries.map { it.toEntity() })
        }
    }

    suspend fun clearAll() {
        db.withTransaction {
            entryDao.deleteAll()
            goalDao.deleteAll()
        }
    }
}

sealed interface DeleteResult {
    data class Deleted(val entry: Entry) : DeleteResult
    data class Failed(val message: String) : DeleteResult
}
