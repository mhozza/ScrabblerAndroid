package eu.hozza.scrabbler.android

import eu.hozza.scrabbler.Scrabbler
import eu.hozza.scrabbler.buildTrie
import eu.hozza.scrabbler.filterDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScrabblerDataService(private val dictionaryDataService: DictionaryDataService) {
    suspend fun findPermutations(word: String): List<String> = withContext(Dispatchers.Default) {
        val dictionary = filterDictionary(checkNotNull(dictionaryDataService.dictionary), word)
        val trie = buildTrie(dictionary)

        Scrabbler(dictionary, trie, true).answer(word, limit = 50)
    }
}