package com.mhozza.scrabbler.android

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhozza.scrabbler.android.ui.ScrabblerTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.nio.file.Paths


@Composable
fun FormAndResultsScreen(resultsState: ResultsState, modifier: Modifier = Modifier, form: @Composable () -> Unit ) {
    Column(
        modifier = modifier
    ) {
       form()
       Spacer(Modifier.height(8.dp))
       HorizontalDivider(thickness = 1.dp)
       Spacer(Modifier.height(8.dp))
       Results(resultsState)
    }
}

@Composable
fun Results(resultsState: ResultsState, modifier: Modifier = Modifier) {
    when (resultsState) {
        is ResultsState.Loading -> Box(
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        is ResultsState.Loaded -> {
            LazyColumn(
                modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (resultsState.results.isEmpty()) {
                    item {
                        Text(
                            "No results",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                items(resultsState.results) { word ->
                    Text(
                        word,
                        modifier = modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        is ResultsState.Idle ->
            Text(
                "Please type a query",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
}

@Preview
@Composable
fun ResultsPreview() {
    ScrabblerTheme {
        Results(ResultsState.Loaded(listOf("foo", "bar")))
    }
}

@Preview
@Composable
fun ResultsIdlePreview() {
    ScrabblerTheme {
        Results(ResultsState.Idle)
    }
}

@Preview
@Composable
fun ResultsLoadingPreview() {
    ScrabblerTheme {
        Results(ResultsState.Loading)
    }
}

@Preview
@Composable
fun ResultsEmptyPreview() {
    ScrabblerTheme {
        Results(ResultsState.Loaded(emptyList()))
    }
}

@Composable
fun SelectDictionaryPrompt(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Please select dictionary.",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
fun SelectDictionaryPromptPreview() {
    ScrabblerTheme {
        SelectDictionaryPrompt(Modifier.size(400.dp))
    }
}

@Composable
fun DictionarySelector(
    snackbarHostState: SnackbarHostState,
    selectedDictionary: String? = null,
    isDictionaryLoading: Boolean = false,
    onDictionarySelected: (String?) -> Unit = {},
    onNewDictionarySelected: (String, String) -> Unit = { _, _ -> run {} },
    onDictionaryDeleted: (String) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var openDialog by remember { mutableStateOf(false) }
    var dictionaryPath: Uri? by remember { mutableStateOf(null) }
    var name by remember { mutableStateOf("") }

    val dictionaries by (LocalContext.current.applicationContext as ScrabblerApplication)
        .dictionaryDataService
        .dictionaries
        .map { it.map { d -> d.name } }
        .collectAsState(listOf())

    val contentResolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()

    val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            name = extractFilename(contentResolver, uri)?.let {
                var uniqueName = generateName(it)
                var attempt = 0
                while (dictionaries.contains(uniqueName)) {
                    uniqueName = generateName(it, ++attempt)
                }
                uniqueName
            } ?: ""
            dictionaryPath = uri
            openDialog = true
        }

    InputDialog(
        initialValue = name,
        openDialog = openDialog,
        validator = { !dictionaries.contains(it) },
        onDismissRequest = { openDialog = false },
    ) {
        openDialog = false
        try {
            onNewDictionarySelected(it, dictionaryPath.toString())
        } catch (e: Exception) {
            Log.e("DictionarySelector", "Failed to load dictionary.", e)
            scope.launch { snackbarHostState.showSnackbar("Failed to load dictionary.") }
        }
    }

    Box(modifier = Modifier.padding(CONTENT_PADDING)) {
        Button(onClick = { expanded = true }) {
            if(isDictionaryLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp).size(18.dp), color = LocalContentColor.current)
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu_book_black_18dp),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(selectedDictionary ?: "Select dictionary")
            Icon(Default.ArrowDropDown, null)
        }
        DropdownMenu(
            modifier = Modifier.fillMaxWidth(),
            expanded = expanded,
            onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text("Load from file.")
                },
                onClick = {
                    expanded = false
                    openFileLauncher.launch(
                        arrayOf(
                            "text/plain",
                            "application/gzip",
                            "text/csv",
                            "text/comma-separated-values"
                        )
                    )
                })
            for (dictionary in dictionaries) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(dictionary, modifier = Modifier.weight(1f))
                            Icon(
                                Default.Delete, contentDescription = "Delete",
                                Modifier
                                    .clickable(onClick = {
                                        expanded = false
                                        onDictionaryDeleted(dictionary)
                                    })
                                    .padding(4.dp)
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onDictionarySelected(dictionary)
                    })
            }
        }
    }
}

fun extractFilename(contentResolver: ContentResolver, uri: Uri): String? {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        return cursor.getString(nameIndex)
    }
    return null
}

fun generateName(path: String, attempt: Int? = null): String {
    val name = Paths.get(path).fileName.toString().let {
        it.substring(0, it.indexOf('.'))
    }
    return if (attempt == null) {
        name
    } else {
        "$name ($attempt)"
    }
}

fun String?.emptyToNull(): String? = if (isNullOrEmpty()) null else this

val CONTENT_PADDING = 8.dp