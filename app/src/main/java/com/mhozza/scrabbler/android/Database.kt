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

    @Query("DELETE FROM dictionary_item")
    suspend fun deleteAll()
}

@Database(entities = [DictionaryItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictionaryItemDao(): DictionaryItemDao

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
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}

