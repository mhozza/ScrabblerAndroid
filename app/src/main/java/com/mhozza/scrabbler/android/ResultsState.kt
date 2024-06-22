package com.mhozza.scrabbler.android

sealed interface ResultsState {
    data object Idle : ResultsState
    data object Loading: ResultsState
    data class Loaded(val results: List<String>): ResultsState
}