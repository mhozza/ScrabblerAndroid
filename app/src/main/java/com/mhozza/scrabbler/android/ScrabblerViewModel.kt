package com.mhozza.scrabbler.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScrabblerViewModel(application: Application) : AndroidViewModel(application) {

    private val _results = MutableStateFlow<List<String>>(emptyList())
    val results: StateFlow<List<String>> = _results

    private val _loadingState = MutableStateFlow(LoadingState.IDLE)
    val loadingState: StateFlow<LoadingState> = _loadingState

    private var resultsJob: Job? = null
    private var lastQuery: ScrabblerQuery? = null
    private var lastDictionary: String? = null

    fun clearResults() {
        _results.value = emptyList()
        _loadingState.value = LoadingState.IDLE
    }

    fun onQueryChanged(dictionary: String, newQuery: ScrabblerQuery) {
        if (newQuery == lastQuery && dictionary == lastDictionary) return
        lastQuery = newQuery
        lastDictionary = dictionary
        clearResults()
        _loadingState.value = LoadingState.LOADING
        synchronized(this) {
            if (resultsJob != null && resultsJob!!.isActive) {
                resultsJob!!.cancel()
            }
            resultsJob = viewModelScope.launch {
                if (newQuery is PermutationsScrabblerQuery) {
                    _results.value =
                        getApplication<ScrabblerApplication>().scrabblerDataService.findPermutations(
                            dictionary, newQuery
                        )
                } else if (newQuery is SearchScrabblerQuery) {
                    _results.value =
                        getApplication<ScrabblerApplication>().scrabblerDataService.search(
                            dictionary, newQuery
                        )
                }
                _loadingState.value = LoadingState.IDLE
            }
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
}
