package com.mhozza.scrabbler.android

import android.content.ContentResolver
import android.net.Uri
import com.mhozza.scrabbler.Scrabbler
import com.mhozza.scrabbler.buildTrie
import com.mhozza.scrabbler.filterDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class ScrabblerDataService(
    dictionaryDataService: DictionaryDataService,
    contentResolver: ContentResolver
) {
    private val dictionaryLoader = DictionaryLoader(dictionaryDataService, contentResolver)

    suspend fun findPermutations(dictionaryName: String, query: ScrabblerQuery): List<String> =
        withContext(Dispatchers.Default) {
            val wildcard = if (query.wildcard) '?' else null
            val dictionary =
                filterDictionary(
                    dictionaryLoader.getDictionary(dictionaryName),
                    query.word,
                    wildcard,
                    useAllLetters = !query.allowShorter,
                    prefix = query.prefix
                )
            val trie = buildTrie(dictionary)

            Scrabbler(dictionary, trie, true).answer(
                query.word,
                limit = 200,
                regex = false,
                allowShorter = query.allowShorter,
                prefix = query.prefix,
                wildcard = wildcard
            )
        }

    private class DictionaryLoader(
        private val dictionaryDataService: DictionaryDataService,
        private val contentResolver: ContentResolver,
    ) {
        private data class Dictionary(val name: String, val dictionary: List<String>)

        var currentDictionary: Dictionary? = null

        suspend fun getDictionary(name: String): List<String> {
            if (currentDictionary?.name != name) {
                currentDictionary = loadDictionary(name)
            }
            return currentDictionary?.dictionary
                ?: throw IllegalStateException("Could not load dictionary")
        }

        private suspend fun loadDictionary(name: String?): Dictionary? {
            val dictionary = if (name == null) {
                null
            } else {
                val uri = dictionaryDataService.getDictionaryUri(name)
                if (uri == null) {
                    null
                } else {
                    loadDictionaryFromFile(uri)
                }
            }
            return if (name != null && dictionary != null) {
                Dictionary(name, dictionary)
            } else {
                null
            }
        }

        private suspend fun loadDictionaryFromFile(fname: String): List<String> =
            withContext(Dispatchers.IO) {
                BufferedReader(InputStreamReader(contentResolver.openInputStream(Uri.parse(fname)))).use {
                    generateSequence {
                        it.readLine()?.trim()?.toLowerCase(Locale.getDefault())
                    }.toList()
                }
            }

    }
}