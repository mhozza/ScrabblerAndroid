package com.mhozza.scrabbler.android

import android.content.ContentResolver
import android.net.Uri
import com.mhozza.scrabbler.Dictionary
import com.mhozza.scrabbler.Scrabbler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

private const val WORD_COUNT_LIMIT = 200

class ScrabblerDataService(
    dictionaryDataService: DictionaryDataService,
    contentResolver: ContentResolver
) {
    private val scrabblerFactory = ScrabblerFactory(dictionaryDataService, contentResolver)

    suspend fun findPermutations(dictionaryName: String, query: ScrabblerQuery): List<String> =
        withContext(Dispatchers.Default) {
            scrabblerFactory.get(dictionaryName, query.removeAccents).findPermutations(
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
        data class ScrabblerCacheKey(val name: String, val removeAccents: Boolean)
        data class CachedScrabbler(val key: ScrabblerCacheKey, val scrabbler: Scrabbler)

        var cachedScrabbler: CachedScrabbler? = null

        suspend fun get(name: String, removeAccents: Boolean = false): Scrabbler =
            withContext(Dispatchers.IO) {
                val key = ScrabblerCacheKey(name, removeAccents)
                if (cachedScrabbler?.key != key) {
                    // Allow the previous scrabbler to be cleared.
                    cachedScrabbler = null
                    val dictionary = loadDictionary(name, removeAccents)
                    cachedScrabbler = CachedScrabbler(key, Scrabbler(dictionary))
                }
                cachedScrabbler?.scrabbler
                    ?: throw IllegalStateException("Could not load dictionary")
            }

        private suspend fun loadDictionary(name: String, removeAccents: Boolean): Dictionary {
            val uri = dictionaryDataService.getDictionaryUri(name)
                ?: throw java.lang.IllegalStateException("Dictionary not found.")
            return loadDictionaryFromFile(uri, removeAccents)
        }

        private fun loadDictionaryFromFile(fname: String, removeAccents: Boolean): Dictionary {
            val uri = Uri.parse(fname)
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                return Dictionary.load(
                    inputStream,
                    compressed = uri.isContentCompressed(),
                    removeAccents = removeAccents
                )
            } else {
                throw RuntimeException("Could not open InputStream")
            }
        }

        private fun Uri.isContentCompressed(): Boolean {
            val inputStream = contentResolver.openInputStream(this)
            return try {
                GZIPInputStream(inputStream)
                true
            } catch (e: ZipException) {
                false
            }
        }
    }
}