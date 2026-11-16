package app.penny.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.penny.domain.Category
import app.penny.domain.Entry
import app.penny.domain.EntryType
import app.penny.domain.Goal
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "target_paise") val targetPaise: Long,
    /** Optional target date as epoch day. */
    @ColumnInfo(name = "target_date") val targetDate: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            // Deleting a goal keeps its money: entries simply become unassigned.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("goal_id"), Index("date")],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Always positive, in paise. Direction is given by [type]. */
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    /** "DEPOSIT" or "WITHDRAWAL". */
    val type: String,
    val category: String,
    val note: String,
    /** Calendar date as epoch day (timezone-independent). */
    val date: Long,
    @ColumnInfo(name = "goal_id") val goalId: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun EntryEntity.toDomain() = Entry(
    id = id,
    amountPaise = amountPaise,
    type = if (type == EntryType.WITHDRAWAL.name) EntryType.WITHDRAWAL else EntryType.DEPOSIT,
    category = Category.fromName(category),
    note = note,
    date = LocalDate.ofEpochDay(date),
    goalId = goalId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Entry.toEntity() = EntryEntity(
    id = id,
    amountPaise = amountPaise,
    type = type.name,
    category = category.name,
    note = note,
    date = date.toEpochDay(),
    goalId = goalId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun GoalEntity.toDomain() = Goal(
    id = id,
    title = title,
    targetPaise = targetPaise,
    targetDate = targetDate?.let(LocalDate::ofEpochDay),
    createdAt = createdAt,
)

fun Goal.toEntity() = GoalEntity(
    id = id,
    title = title,
    targetPaise = targetPaise,
    targetDate = targetDate?.toEpochDay(),
    createdAt = createdAt,
)
