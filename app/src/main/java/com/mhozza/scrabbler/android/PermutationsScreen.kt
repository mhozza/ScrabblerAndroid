package com.mhozza.scrabbler.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.mhozza.scrabbler.android.ui.ScrabblerTheme
import kotlinx.serialization.Serializable

@Serializable
object PermutationsScreenDestination

@Composable fun PermutationsScreen(removeAccents: Boolean, scrabblerViewModel: ScrabblerViewModel, modifier: Modifier =Modifier, ) {
    val resultsState by scrabblerViewModel.resultsState.collectAsState()

    FormAndResultsScreen(resultsState, modifier) {
        PermutationsForm(
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
fun PermutationsForm(
    removeAccents: Boolean,
    onRemoveAccentsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onQueryChanged: (ScrabblerQuery) -> Unit = {},
    onClearResults: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .animateContentSize()
    ) {
        val keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search)

        var collapsed by rememberSaveable { mutableStateOf(false) }

        var wordFieldState by rememberSaveable { mutableStateOf("") }
        var prefixFieldState by rememberSaveable { mutableStateOf("") }
        var containsField by rememberSaveable { mutableStateOf("") }
        var suffixFieldState by rememberSaveable { mutableStateOf("") }
        var regexFilterField by rememberSaveable { mutableStateOf("") }
        var useAllLettersField by rememberSaveable { mutableStateOf(true) }

        SideEffect {
            if (wordFieldState.isEmpty()) {
                onClearResults()
            }
        }

        val onSubmit = {
            val query =
                PermutationsScrabblerQuery(
                    word = wordFieldState,
                    prefix = prefixFieldState,
                    suffix = suffixFieldState,
                    contains = containsField,
                    regexFilter = regexFilterField.emptyToNull(),
                    useAllLetters = useAllLettersField,
                )
            onQueryChanged(query)
            collapsed = true
        }

        val defaultFieldModifier = Modifier.fillMaxWidth()

        Row(defaultFieldModifier, verticalAlignment = Alignment.CenterVertically) {
            TextFormWidget(
                wordFieldState,
                onValueChange = { wordFieldState = it },
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(),
                label = "Word",
                keyboardOptions = keyboardOptions,
                onClick = { collapsed = false },
                onSubmit = onSubmit
            )
            AnimatedVisibility(collapsed) {
                IconButton(onClick = { collapsed = false }) {
                    Icon(
                        Default.ArrowDropDown,
                        "Expand form."
                    )
                }
            }
        }

        if (!collapsed) {
            TextFormWidget(
                prefixFieldState,
                onValueChange = { prefixFieldState = it },
                modifier = defaultFieldModifier,
                label = "Prefix",
                keyboardOptions = keyboardOptions
            )
            TextFormWidget(
                containsField,
                onValueChange = { containsField = it },
                modifier = defaultFieldModifier,
                label = "Contains",
                keyboardOptions = keyboardOptions
            )
            TextFormWidget(
                suffixFieldState,
                onValueChange = { suffixFieldState = it },
                modifier = defaultFieldModifier,
                label = "Suffix",
                keyboardOptions = keyboardOptions
            )
            TextFormWidget(
                regexFilterField,
                onValueChange = { regexFilterField = it },
                modifier = defaultFieldModifier,
                label = "Filter (regex)",
                keyboardOptions = keyboardOptions
            )
            BooleanFormWidget(
                useAllLettersField,
                onValueChange = { useAllLettersField = it },
                modifier = defaultFieldModifier,
                label = "Use all letters"
            )
            BooleanFormWidget(
                removeAccents,
                onValueChange = onRemoveAccentsChanged,
                modifier = defaultFieldModifier,
                label = "Remove accents"
            )
            Button(
                modifier = defaultFieldModifier,
                onClick = {
                    onSubmit()
                },
            ) {
                Text("Search")
            }
        }
    }
}


@Preview
@Composable
fun PermutationsFormPreview() {
    ScrabblerTheme {
        PermutationsForm(true, {})
    }
}