package eu.hozza.scrabbler.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ScrabblerApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }

    val dictionaryDataService by lazy { DictionaryDataService(database.dictionaryItemDao(), contentResolver) }
    val scrabblerDataService by lazy { ScrabblerDataService(dictionaryDataService, contentResolver) }
}