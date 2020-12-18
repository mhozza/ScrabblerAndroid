package eu.hozza.scrabbler.android

import android.content.Context
import android.net.Uri
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.nio.file.Paths

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

//    @Transaction
//    suspend fun insertByPath(path: String): DictionaryItem {
//
//        var dictionaryItem = getByPath(path.toString())
//        if (dictionaryItem != null) {
//            return dictionaryItem
//        }
//        var attempt = 0
//        var name = generateName(path)
//        while (getByName(name) != null) {
//            name = generateName(path, attempt++)
//        }
//        dictionaryItem = DictionaryItem(name, path.toString())
//        insertAll(dictionaryItem)
//        return dictionaryItem
//    }

    @Delete
    suspend fun delete(dictionaryItem: DictionaryItem)

    @Query("DELETE FROM dictionary_item")
    suspend fun deleteAll()
}

@Database(entities = [DictionaryItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictionaryItemDao(): DictionaryItemDao

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    val dictionaryItemDao = database.dictionaryItemDao()
                    dictionaryItemDao.deleteAll()
                    dictionaryItemDao.insertAll(
                        DictionaryItem("Foo", "foo"),
                        DictionaryItem("Bar", "bar"),
                    )
                }
            }
        }
    }


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
//                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}

