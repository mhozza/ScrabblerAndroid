package com.mhozza.scrabbler.android

import android.app.Application

class ScrabblerApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }

    val dictionaryDataService by lazy { DictionaryDataService(database.dictionaryItemDao(), contentResolver) }
    val scrabblerDataService by lazy { ScrabblerDataService(dictionaryDataService, contentResolver) }
}