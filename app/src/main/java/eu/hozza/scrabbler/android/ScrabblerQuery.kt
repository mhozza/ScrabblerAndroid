package eu.hozza.scrabbler.android

data class ScrabblerQuery(
    val word: String,
    val wildcard: Boolean = false,
    val prefix: String? = null,
    val allowShorter: Boolean = false,
)