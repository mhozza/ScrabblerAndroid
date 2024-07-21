package com.mhozza.scrabbler.android

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.IllegalArgumentException

class SettingsDataService(private val settingsDao: SettingsDao) {
    val selectedDictionary = StringSettingAccessor(SELECTED_DICTIONARY_KEY)

    open inner class SettingAccessor<T>(
        private val key: String,
        private val setTransform: (T) -> String? = { it.toString() },
        private val getTransform: (String?) -> T,
    ) {
        fun get(): Flow<T> {
            return settingsDao.get(key).map { getTransform(it) }
        }

        suspend fun set(value: T) {
            val v = setTransform(value)
            if (v == null) {
                settingsDao.delete(key)
            } else {
                settingsDao.set(key to v)
            }
        }
    }
    inner class StringSettingAccessor(key: String): SettingAccessor<String?>(key, {it}, {it})

    companion object {
        private const val SELECTED_DICTIONARY_KEY = "selected_dictionary"
        private const val SELECTED_MODE_KEY = "selected_mode"
    }
}

