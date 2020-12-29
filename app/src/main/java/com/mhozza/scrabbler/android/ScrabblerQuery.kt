package com.mhozza.scrabbler.android

data class ScrabblerQuery(
    val word: String,
    val prefix: String? = null,
    val suffix: String? = null,
    val contains: String? = null,
    val regexFilter: String? = null,
    val useAllLetters: Boolean = true,
)