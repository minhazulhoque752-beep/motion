package app.penny.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EntryEntity::class, GoalEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PennyDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun goalDao(): GoalDao

    companion object {
        const val NAME = "penny.db"

        fun build(context: Context): PennyDatabase =
            Room.databaseBuilder(context.applicationContext, PennyDatabase::class.java, NAME)
                // Future schema changes must ship explicit migrations; never silently wipe financial data.
                .build()
    }
}
