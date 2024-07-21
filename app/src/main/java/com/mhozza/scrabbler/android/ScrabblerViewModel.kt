package com.mhozza.scrabbler.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

class ScrabblerViewModel(application: Application, private val savedStateHandle: SavedStateHandle) :
    AndroidViewModel(application) {
    private val scrabblerApplication = getApplication<ScrabblerApplication>()

    private val requestChannel = Channel<DictionaryQuery?>()

    val removeAccents: StateFlow<Boolean> = savedStateHandle.getStateFlow(REMOVE_ACCENTS_KEY, true)
    private val _isDictionaryLoading = MutableStateFlow(false)
    val isDictionaryLoading = _isDictionaryLoading.asStateFlow()

    val selectedDictionary: StateFlow<String?> =
        getApplication<ScrabblerApplication>().settingsDataService.selectedDictionary.get()
            .map {
                val result =
                    if (it == null || scrabblerApplication.dictionaryDataService.getDictionaryUri(it) == null)
                        null
                    else
                        it
                if (result != null) {
                    viewModelScope.launch { preloadDictionary(result, removeAccents.value) }
                }
                result
            }.stateIn(
                viewModelScope, SharingStarted.Lazily, null,
            )

    val resultsState: StateFlow<ResultsState> =
        requestChannel.consumeAsFlow().distinctUntilChanged().transform { dictionaryQuery ->
            emit(ResultsState.Loading)
            if (dictionaryQuery == null) {
                emit(ResultsState.Idle)
                return@transform
            }
            val results = when (dictionaryQuery.query) {
                is PermutationsScrabblerQuery -> {
                    scrabblerApplication.scrabblerDataService.findPermutations(
                        dictionaryQuery.dictionary, dictionaryQuery.removeAccents, dictionaryQuery.query,
                    )
                }

                is SearchScrabblerQuery -> {
                    scrabblerApplication.scrabblerDataService.search(
                        dictionaryQuery.dictionary, dictionaryQuery.removeAccents, dictionaryQuery.query
                    )
                }
            }
            emit(ResultsState.Loaded(results))
        }.stateIn(viewModelScope, SharingStarted.Lazily, ResultsState.Idle)

    fun setRemoveAccents(value: Boolean) {
        savedStateHandle[REMOVE_ACCENTS_KEY] = value
        viewModelScope.launch {
            val name = selectedDictionary.value
            if(name != null) {
                preloadDictionary(name, value)
            }
        }
    }

    fun clearResults() {
        viewModelScope.launch {
            requestChannel.send(null)
        }
    }

    fun onQueryChanged(query: ScrabblerQuery) {
        viewModelScope.launch {
            requestChannel.send(DictionaryQuery(requireNotNull(selectedDictionary.value), removeAccents.value, query))
        }
    }

    fun onSelectNewDictionary(name: String?) {
        scrabblerApplication.applicationScope.launch {
            scrabblerApplication.settingsDataService.selectedDictionary.set(name)
        }
    }

    fun onLoadNewDictionary(name: String, fname: String) {
        viewModelScope.launch {
            scrabblerApplication.dictionaryDataService.loadDictionary(
                name,
                fname
            )
        }
    }

    private suspend fun preloadDictionary(name: String, removeAccents: Boolean) =
        coroutineScope {
            _isDictionaryLoading.value = true
            scrabblerApplication.scrabblerDataService.preloadDictionary(
                name,
                removeAccents,
            )
            _isDictionaryLoading.value = false
        }

    private data class DictionaryQuery(val dictionary: String, val removeAccents: Boolean, val query: ScrabblerQuery)

    companion object {
        private const val REMOVE_ACCENTS_KEY = "REMOVE_ACCENTS_KEY"
    }
}

