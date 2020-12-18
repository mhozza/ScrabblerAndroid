package eu.hozza.scrabbler.android

import android.content.ContentResolver
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class DictionaryDataService(
    private val dictionaryItemDao: DictionaryItemDao,
    private val contentResolver: ContentResolver,
) {
    var currentDictionaryName: String? = null
        private set
    var dictionary: List<String>? = null
        private set
    val dictionaries = dictionaryItemDao.getAll()

    suspend fun loadDictionary(name: String, fname: String): String {
        val dictionaryItem = DictionaryItem(name, fname)
        try {
            dictionaryItemDao.insertAll(dictionaryItem)
        } catch (e: SQLiteConstraintException) {
            // TODO
        }
        return selectDictionary(dictionaryItem.name)
    }

    suspend fun selectDictionary(name: String): String {
        val dictionaryItem = dictionaryItemDao.getByName(name)
        checkNotNull(dictionaryItem)
        loadDictionaryFromFile(dictionaryItem.path)
        currentDictionaryName = name
        return name
    }

    private suspend fun loadDictionaryFromFile(fname: String) {
        withContext(Dispatchers.IO) {
            BufferedReader(InputStreamReader(contentResolver.openInputStream(Uri.parse(fname)))).use {
                dictionary = generateSequence { it.readLine()?.trim()?.toLowerCase(Locale.getDefault()) }.toList()
            }
        }
    }
}