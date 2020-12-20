package eu.hozza.scrabbler.android

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch


class ScrabblerViewModel(application: Application) : AndroidViewModel(application) {

    private val _results = MutableLiveData(listOf<String>())
    val results: LiveData<List<String>> = _results

    val dictionaries: LiveData<List<DictionaryItem>> =
        getApplication<ScrabblerApplication>().dictionaryDataService.dictionaries.asLiveData()
    private val _selectedDictionary: MutableLiveData<String> =
        MutableLiveData(getApplication<ScrabblerApplication>().dictionaryDataService.currentDictionaryName)
    val selectedDictionary: LiveData<String> = _selectedDictionary

    fun onQueryChanged(newQuery: ScrabblerQuery) {
        viewModelScope.launch {
            _results.value =
                getApplication<ScrabblerApplication>().scrabblerDataService.findPermutations(
                    newQuery
                )
        }
    }

    fun onDictionarySelected(dictionaryName: String) {
        viewModelScope.launch {
            _selectedDictionary.value =
                getApplication<ScrabblerApplication>().dictionaryDataService.selectDictionary(
                    dictionaryName
                )
        }
    }

    fun onLoadNewDictionary(name: String, fname: String) {
        viewModelScope.launch {
            _selectedDictionary.value =
                getApplication<ScrabblerApplication>().dictionaryDataService.loadDictionary(
                    name,
                    fname
                )
        }
    }
}
