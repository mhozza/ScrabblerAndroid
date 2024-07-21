package com.mhozza.scrabbler.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.mhozza.scrabbler.android.ui.ScrabblerTheme
import kotlinx.serialization.Serializable

@Serializable
object SearchScreenDestination

@Composable
fun SearchScreen(removeAccents: Boolean,  scrabblerViewModel: ScrabblerViewModel, modifier: Modifier = Modifier,) {
    val resultsState by scrabblerViewModel.resultsState.collectAsState()

    FormAndResultsScreen(resultsState, modifier) {
        SearchForm(
            removeAccents = removeAccents,
            onRemoveAccentsChanged = {scrabblerViewModel.setRemoveAccents(it)},
            onQueryChanged = { query ->
                scrabblerViewModel.onQueryChanged(
                    query,
                )
            },
            onClearResults = { scrabblerViewModel.clearResults() })
    }
}

@Composable
fun SearchForm(
    removeAccents: Boolean,
    onRemoveAccentsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onQueryChanged: (ScrabblerQuery) -> Unit = {},
    onClearResults: () -> Unit = {},
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        val keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search)

        var wordFieldState by rememberSaveable { mutableStateOf("") }

        SideEffect {
            if (wordFieldState.isEmpty()) {
                onClearResults()
            }
        }

        val defaultFieldModifer = Modifier.fillMaxWidth()

        TextFormWidget(
            wordFieldState,
            onValueChange = { wordFieldState = it },
            modifier = defaultFieldModifer,
            label = "Word",
            keyboardOptions = keyboardOptions
        )
        BooleanFormWidget(
            removeAccents,
            onValueChange = onRemoveAccentsChanged,
            modifier = defaultFieldModifer,
            label = "Remove accents"
        )

        Button(
            modifier = defaultFieldModifer,
            onClick = {
                val query =
                    SearchScrabblerQuery(
                        word = wordFieldState,
                    )
                onQueryChanged(query)
            },
        ) {
            Text("Search")
        }
    }
}


@Preview
@Composable
fun SearchFormPreview() {
    ScrabblerTheme {
        SearchForm(true, {})
    }
}