package eu.hozza.scrabbler.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ScrabblerViewModel(application: Application) : AndroidViewModel(application) {

    private val _results: MutableLiveData<List<String>> = MutableLiveData(null)
    val results: LiveData<List<String>> = _results

    private val _loadingState = MutableLiveData(LoadingState.IDLE)
    val loadingState: LiveData<LoadingState> = _loadingState

    fun onQueryChanged(dictionary: String, newQuery: ScrabblerQuery) {
        _loadingState.value = LoadingState.LOADING
        _results.value = null
        viewModelScope.launch {
            _results.value =
                getApplication<ScrabblerApplication>().scrabblerDataService.findPermutations(
                    dictionary, newQuery
                )
            _loadingState.value = LoadingState.IDLE
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
