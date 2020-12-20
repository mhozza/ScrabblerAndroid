package eu.hozza.scrabbler.android

import eu.hozza.scrabbler.Scrabbler
import eu.hozza.scrabbler.buildTrie
import eu.hozza.scrabbler.filterDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScrabblerDataService(private val dictionaryDataService: DictionaryDataService) {
    suspend fun findPermutations(query: ScrabblerQuery): List<String> =
        withContext(Dispatchers.Default) {
            val wildcard = if (query.wildcard) '?' else null
            val dictionary =
                filterDictionary(checkNotNull(dictionaryDataService.dictionary), query.word, wildcard, useAllLetters = !query.allowShorter, prefix = query.prefix )
            val trie = buildTrie(dictionary)

            Scrabbler(dictionary, trie, true).answer(
                query.word,
                limit = 50,
                regex = false,
                allowShorter = query.allowShorter,
                prefix = query.prefix,
                wildcard = wildcard
            )
        }
}