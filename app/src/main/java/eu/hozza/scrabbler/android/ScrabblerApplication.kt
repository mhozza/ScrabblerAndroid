package eu.hozza.scrabbler.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ScrabblerApplication : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    private val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    val dictionaryDataService by lazy { DictionaryDataService(database.dictionaryItemDao(), this.contentResolver) }
    val scrabblerDataService by lazy { ScrabblerDataService(dictionaryDataService) }
}