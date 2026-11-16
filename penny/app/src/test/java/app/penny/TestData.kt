package app.penny

import app.penny.domain.Category
import app.penny.domain.Entry
import app.penny.domain.EntryType
import app.penny.domain.Goal
import java.time.LocalDate

private var nextId = 1L

fun deposit(rupees: Long, date: LocalDate, goalId: Long? = null, category: Category = Category.GENERAL, note: String = "", paise: Long = 0) =
    entry(rupees * 100 + paise, EntryType.DEPOSIT, date, goalId, category, note)

fun withdrawal(rupees: Long, date: LocalDate, goalId: Long? = null, category: Category = Category.GENERAL, note: String = "") =
    entry(rupees * 100, EntryType.WITHDRAWAL, date, goalId, category, note)

fun entry(paise: Long, type: EntryType, date: LocalDate, goalId: Long?, category: Category, note: String): Entry {
    val id = nextId++
    return Entry(id, paise, type, category, note, date, goalId, createdAt = id, updatedAt = id)
}

fun goal(id: Long, title: String, targetRupees: Long, targetDate: LocalDate? = null) =
    Goal(id, title, targetRupees * 100, targetDate, createdAt = id)

fun d(y: Int, m: Int, day: Int): LocalDate = LocalDate.of(y, m, day)
