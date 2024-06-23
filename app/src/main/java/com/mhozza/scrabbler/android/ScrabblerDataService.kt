package com.mhozza.scrabbler.android

import android.content.ContentResolver
import android.net.Uri
import com.mhozza.scrabbler.Dictionary
import com.mhozza.scrabbler.Scrabbler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

private const val WORD_COUNT_LIMIT = 200

class ScrabblerDataService(
    dictionaryDataService: DictionaryDataService,
    contentResolver: ContentResolver
) {
    private val scrabblerFactory = ScrabblerFactory(dictionaryDataService, contentResolver)

    suspend fun preloadDictionary(dictionaryName: String, removeAccents: Boolean) {
        withContext(Dispatchers.Default) {
            scrabblerFactory.get(dictionaryName, removeAccents)
        }
    }

    suspend fun findPermutations(dictionaryName: String, query: PermutationsScrabblerQuery): List<String> =
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

    suspend fun search(dictionaryName: String, query: SearchScrabblerQuery): List<String> =
        withContext(Dispatchers.Default) {
            scrabblerFactory.get(dictionaryName, query.removeAccents).findByRegex(
                query.word,
                limit = WORD_COUNT_LIMIT,
            )
        }

    private class ScrabblerFactory(
        private val dictionaryDataService: DictionaryDataService,
        private val contentResolver: ContentResolver,
        private val allowMultipleScrabblers: Boolean = true,
    ) {
        data class ScrabblerCacheKey(val name: String, val removeAccents: Boolean)
        data class CachedScrabbler(val key: ScrabblerCacheKey, val scrabbler: Scrabbler)

        var cachedScrabbler: CachedScrabbler? = null
        var cachedScrabblerNoAccents: CachedScrabbler? = null

        private val mutexWithAccents = Mutex()
        private val mutexWithoutAccents = Mutex()

        suspend fun get(name: String, removeAccents: Boolean = false): Scrabbler {
            if(!allowMultipleScrabblers) {
                cachedScrabbler = null
                cachedScrabblerNoAccents = null
            }
            return if(removeAccents) {
                getWithoutAccents(name)
            } else {
                getWithAccents(name)
            }
        }

        suspend fun getWithAccents(name: String): Scrabbler =
            withContext(Dispatchers.IO) {
                val key = ScrabblerCacheKey(name, false)
                mutexWithAccents.withLock {
                    if (cachedScrabbler?.key != key) {
                        // Allow the previous scrabbler to be cleared.
                        cachedScrabbler = null
                        val dictionary = loadDictionary(name, false)
                        cachedScrabbler = CachedScrabbler(key, Scrabbler(dictionary))
                    }
                }
                cachedScrabbler?.scrabbler
                    ?: throw IllegalStateException("Could not load dictionary")
            }

        suspend fun getWithoutAccents(name: String): Scrabbler =
            withContext(Dispatchers.IO) {
                val key = ScrabblerCacheKey(name, true)
                mutexWithoutAccents.withLock {
                    if (cachedScrabblerNoAccents?.key != key) {
                        // Allow the previous scrabbler to be cleared.
                        cachedScrabblerNoAccents = null
                        val dictionary = loadDictionary(name, true)
                        cachedScrabblerNoAccents = CachedScrabbler(key, Scrabbler(dictionary))
                    }
                }
                cachedScrabblerNoAccents?.scrabbler
                    ?: throw IllegalStateException("Could not load dictionary")
            }

        private suspend fun loadDictionary(name: String, removeAccents: Boolean): Dictionary {
            val uri = dictionaryDataService.getDictionaryUri(name)
                ?: throw java.lang.IllegalStateException("Dictionary not found: `$name`.")
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