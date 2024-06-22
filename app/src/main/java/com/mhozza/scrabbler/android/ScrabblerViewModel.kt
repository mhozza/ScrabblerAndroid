package com.mhozza.scrabbler.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

class ScrabblerViewModel(application: Application) : AndroidViewModel(application) {
    private val requestChannel = Channel<DictionaryQuery?>()

    val resultsState: StateFlow<ResultsState> = requestChannel.consumeAsFlow().distinctUntilChanged().transform { dictionaryQuery ->
        emit(ResultsState.Loading)
        if (dictionaryQuery == null) {
            emit(ResultsState.Idle)
            return@transform
        }
        val results = when (dictionaryQuery.query) {
            is PermutationsScrabblerQuery -> {
                getApplication<ScrabblerApplication>().scrabblerDataService.findPermutations(
                    dictionaryQuery.dictionary, dictionaryQuery.query
                )
            }
            is SearchScrabblerQuery -> {
                getApplication<ScrabblerApplication>().scrabblerDataService.search(
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

    fun onQueryChanged(dictionary: String, query: ScrabblerQuery) {
        viewModelScope.launch {
            requestChannel.send(DictionaryQuery(dictionary, query))
        }
    }

    fun onLoadNewDictionary(name: String, fname: String) {
        viewModelScope.launch {
            getApplication<ScrabblerApplication>().dictionaryDataService.loadDictionary(
                name,
                fname
            )
        }
    }

    private data class DictionaryQuery(val dictionary: String, val query: ScrabblerQuery, )
}
