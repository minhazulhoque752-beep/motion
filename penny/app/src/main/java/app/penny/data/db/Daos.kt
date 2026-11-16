package app.penny.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY date DESC, created_at DESC, id DESC")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY date DESC, created_at DESC, id DESC")
    suspend fun getAll(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): EntryEntity?

    @Query("SELECT COUNT(*) FROM entries WHERE date = :epochDay")
    suspend fun countOnDay(epochDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: EntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<EntryEntity>)

    @Update
    suspend fun update(entry: EntryEntity): Int

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM entries")
    suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(goals: List<GoalEntity>)

    @Update
    suspend fun update(goal: GoalEntity): Int

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
