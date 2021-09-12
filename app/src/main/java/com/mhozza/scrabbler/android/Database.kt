package com.mhozza.scrabbler.android

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dictionary_item")
data class DictionaryItem(
    @PrimaryKey @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "path") val path: String
)

@Dao
interface DictionaryItemDao {
    @Query("SELECT * FROM dictionary_item ORDER BY name ASC")
    fun getAll(): Flow<List<DictionaryItem>>

    @Query("SELECT * FROM dictionary_item WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): DictionaryItem?

    @Query("SELECT * FROM dictionary_item WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): DictionaryItem?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(vararg dictionaryItem: DictionaryItem)

    @Delete
    suspend fun delete(dictionaryItem: DictionaryItem)

    @Query("DELETE FROM dictionary_item WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM dictionary_item")
    suspend fun deleteAll()
}

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey @ColumnInfo(name = "key") val name: String,
    @ColumnInfo(name = "value") val value: String
)

infix fun String.to(other: String) = Setting(this, other)

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE key = :key LIMIT 1")
    fun get(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAll(vararg setting: Setting)

    @Delete
    suspend fun delete(setting: Setting)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun delete(key: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(setting: Setting)
}

@Database(
    entities = [DictionaryItem::class, Setting::class],
    version = 2,
    autoMigrations = [
//        AutoMigration (from = 1, to = 2),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictionaryItemDao(): DictionaryItemDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}

