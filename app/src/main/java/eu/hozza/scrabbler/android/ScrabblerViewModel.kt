package eu.hozza.scrabbler.android

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class ScrabblerViewModel(application: Application) : AndroidViewModel(application) {

    private val _results = MutableLiveData(listOf<String>())
    val results: LiveData<List<String>> = _results

    fun onQueryChanged(dictionary: String, newQuery: ScrabblerQuery) {
        viewModelScope.launch {
            _results.value =
                getApplication<ScrabblerApplication>().scrabblerDataService.findPermutations(
                    dictionary, newQuery
                )
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
