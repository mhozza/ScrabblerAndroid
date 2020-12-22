package com.mhozza.scrabbler.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScrabblerViewModel(application: Application) : AndroidViewModel(application) {

    private val _results: MutableLiveData<List<String>> = MutableLiveData(null)
    val results: LiveData<List<String>> = _results

    private val _loadingState = MutableLiveData(LoadingState.IDLE)
    val loadingState: LiveData<LoadingState> = _loadingState

    private var resultsJob: Job? = null
    private var lastQuery: ScrabblerQuery? = null

    fun clearResults() {
        _results.value = null
        _loadingState.value = LoadingState.IDLE
    }

    fun onQueryChanged(dictionary: String, newQuery: ScrabblerQuery) {
        if (newQuery == lastQuery) return
        lastQuery = newQuery
        clearResults()
        _loadingState.value = LoadingState.LOADING
        synchronized(this) {
            if (resultsJob != null && resultsJob!!.isActive) {
                resultsJob!!.cancel()
            }
            resultsJob = viewModelScope.launch {
                _results.value =
                    getApplication<ScrabblerApplication>().scrabblerDataService.findPermutations(
                        dictionary, newQuery
                    )
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
