package com.mhozza.scrabbler.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
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

class ScrabblerViewModel(application: Application) : AndroidViewModel(application) {
    private val scrabblerApplication = getApplication<ScrabblerApplication>()

    private val requestChannel = Channel<DictionaryQuery?>()

    private val _dictionaryLoadedState = MutableStateFlow(DictionaryLoadedState.IDLE)
    val dictionaryLoadedState = _dictionaryLoadedState.asStateFlow()
    val selectedDictionary: StateFlow<String?>  = getApplication<ScrabblerApplication>().settingsDataService.selectedDictionary.get().map {
        val result = if (it == null || scrabblerApplication.dictionaryDataService.getDictionaryUri(it) == null)
            null
        else
            it
        if(result!= null) {
            preloadDictionary(result)
        }
        result

    }.stateIn(
        viewModelScope, SharingStarted.Lazily, null,
    )

    val resultsState: StateFlow<ResultsState> = requestChannel.consumeAsFlow().distinctUntilChanged().transform { dictionaryQuery ->
        emit(ResultsState.Loading)
        if (dictionaryQuery == null) {
            emit(ResultsState.Idle)
            return@transform
        }
        val results = when (dictionaryQuery.query) {
            is PermutationsScrabblerQuery -> {
                scrabblerApplication.scrabblerDataService.findPermutations(
                    dictionaryQuery.dictionary, dictionaryQuery.query
                )
            }
            is SearchScrabblerQuery -> {
                scrabblerApplication.scrabblerDataService.search(
                    dictionaryQuery.dictionary, dictionaryQuery.query
                )
            }
        }
        emit(ResultsState.Loaded(results))
    }.stateIn(viewModelScope, SharingStarted.Lazily, ResultsState.Idle)

    fun clearResults() {
        viewModelScope.launch {
            requestChannel.send(null)
        }
    }

    fun onQueryChanged(query: ScrabblerQuery) {
        viewModelScope.launch {
            val selectedDictionaryValue = selectedDictionary.value
            requireNotNull(selectedDictionaryValue)
            requestChannel.send(DictionaryQuery(selectedDictionaryValue, query))
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

    private fun preloadDictionary(name: String) {
        viewModelScope.launch {
            _dictionaryLoadedState.value = DictionaryLoadedState.LOADING
            val nonAccentDictionaryJob = launch {
                scrabblerApplication.scrabblerDataService.preloadDictionary(
                    name,
                    true
                )
            }
            val accentDictionaryJob = launch {
                scrabblerApplication.scrabblerDataService.preloadDictionary(
                    name,
                    false
                )
            }
            nonAccentDictionaryJob.join()
            accentDictionaryJob.join()
            _dictionaryLoadedState.value = DictionaryLoadedState.LOADED
        }
    }

    private data class DictionaryQuery(val dictionary: String, val query: ScrabblerQuery, )
}

enum class DictionaryLoadedState {
    IDLE,
    LOADING,
    LOADED
}