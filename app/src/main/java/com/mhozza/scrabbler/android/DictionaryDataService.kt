package com.mhozza.scrabbler.android

import android.content.ContentResolver
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class DictionaryDataService(
    private val dictionaryItemDao: DictionaryItemDao,
    private val settingsDao: SettingsDao,
    private val contentResolver: ContentResolver,
) {
    val dictionaries = dictionaryItemDao.getAll().filterExisting()

    suspend fun loadDictionary(name: String, fname: String): String {
        val dictionaryItem = DictionaryItem(name, fname)
        try {
            dictionaryItemDao.insertAll(dictionaryItem)
        } catch (e: SQLiteConstraintException) {
            throw IllegalArgumentException("Could not create dictionary item entry.", e)
        }
        return dictionaryItem.name
    }

    suspend fun deleteDictionary(name: String) {
        dictionaryItemDao.delete(name)
    }

    suspend fun getDictionaryUri(name: String): String? {
        return dictionaryItemDao.getByName(name)?.path
    }

    fun getDefaultDictionary(): Flow<String?> {
        return settingsDao.get(DEFAULT_DICTIONARY_KEY)
    }

    suspend fun setDefaultDictionary(dictionaryName: String?) {
        if (dictionaryName == null) {
            settingsDao.delete(DEFAULT_DICTIONARY_KEY)
        } else {
            settingsDao.set(DEFAULT_DICTIONARY_KEY to dictionaryName)
        }
    }

    private fun Flow<List<DictionaryItem>>.filterExisting(): Flow<List<DictionaryItem>> {
        return this.map { fileList ->
            withContext(Dispatchers.IO) {
                fileList.filter { item ->
                    try {
                        val inputStream = contentResolver.openInputStream(Uri.parse(item.path))
                        inputStream?.close()
                        inputStream != null
                    } catch (e: FileNotFoundException) {
                        launch {
                            try {
                                dictionaryItemDao.delete(item)
                            } catch (e: Exception) {
                                Log.w("DictionaryDataService", e)
                            }
                        }
                        false
                    } catch (e: Exception) {
                        Log.e("DictionaryDataService", "Failed to check if file exists: $item", e)
                        false
                    }
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_DICTIONARY_KEY = "default_dictionary"
    }
}