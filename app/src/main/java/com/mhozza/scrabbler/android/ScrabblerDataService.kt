package com.mhozza.scrabbler.android

import android.content.ContentResolver
import android.net.Uri
import com.mhozza.scrabbler.Dictionary
import com.mhozza.scrabbler.Scrabbler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val WORD_COUNT_LIMIT = 200

class ScrabblerDataService(
    dictionaryDataService: DictionaryDataService,
    contentResolver: ContentResolver
) {
    private val scrabblerFactory = ScrabblerFactory(dictionaryDataService, contentResolver)

    suspend fun findPermutations(dictionaryName: String, query: ScrabblerQuery): List<String> =
        withContext(Dispatchers.Default) {
            scrabblerFactory.get(dictionaryName).findPermutations(
                query.word,
                prefix = query.prefix,
                suffix = query.suffix,
                contains = query.contains,
                regexFilter = query.regexFilter,
                useAllLetters = query.useAllLetters,
                limit = WORD_COUNT_LIMIT,
            )
        }

    private class ScrabblerFactory(
        private val dictionaryDataService: DictionaryDataService,
        private val contentResolver: ContentResolver,
    ) {
        data class CachedScrabbler(val key: String, val scrabbler: Scrabbler)

        var cachedScrabbler: CachedScrabbler? = null

        suspend fun get(name: String): Scrabbler {
            if (cachedScrabbler?.key != name) {
                cachedScrabbler = CachedScrabbler(name, Scrabbler(loadDictionary(name)))
            }
            return cachedScrabbler?.scrabbler
                ?: throw IllegalStateException("Could not load dictionary")
        }

        private suspend fun loadDictionary(name: String): Dictionary {
            val uri = dictionaryDataService.getDictionaryUri(name)
                ?: throw java.lang.IllegalStateException("Dictionary not found.")
            return loadDictionaryFromFile(uri)
        }

        private suspend fun loadDictionaryFromFile(fname: String): Dictionary =
            withContext(Dispatchers.IO) {
                val inputStream = contentResolver.openInputStream(Uri.parse(fname))
                if (inputStream != null) {
                    Dictionary.load(inputStream)
                } else {
                    throw RuntimeException("Could not open InputStream")
                }
            }

    }
}