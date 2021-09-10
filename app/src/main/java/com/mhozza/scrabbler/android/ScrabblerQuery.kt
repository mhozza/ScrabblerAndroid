package com.mhozza.scrabbler.android

sealed class ScrabblerQuery()

data class PermutationsScrabblerQuery(
    val word: String,
    val prefix: String? = null,
    val suffix: String? = null,
    val contains: String? = null,
    val regexFilter: String? = null,
    val useAllLetters: Boolean = true,
    val removeAccents: Boolean = true,
): ScrabblerQuery()

data class SearchScrabblerQuery(
    val word: String,
    val removeAccents: Boolean = true,
): ScrabblerQuery()
