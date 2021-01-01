package com.mhozza.scrabbler.android

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhozza.scrabbler.android.ui.ScrabblerTheme
import kotlinx.coroutines.launch
import java.nio.file.Paths

private val CONTENT_PADDING = 8.dp

@ExperimentalAnimationApi
@Composable
fun ScrabblerApp(scrabblerViewModel: ScrabblerViewModel) {
    val scaffoldState = rememberScaffoldState()
    var selectedDictionary: String? by savedInstanceState { null }
    val application = AmbientContext.current.applicationContext as ScrabblerApplication

    Scaffold(scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.primary,
                title = { Text("Scrabbler") },
                actions = {
                    DictionarySelector(
                        scaffoldState,
                        selectedDictionary,
                        onDictionarySelected = {
                            selectedDictionary = it
                        },
                        onNewDictionarySelected = { name, path ->
                            scrabblerViewModel.onLoadNewDictionary(name, path)
                            selectedDictionary = name
                        },
                        onDictionaryDeleted = {
                            if (it == selectedDictionary) {
                                selectedDictionary = null
                            }
                            with(application) {
                                applicationScope.launch {
                                    dictionaryDataService.deleteDictionary(it)
                                }
                            }
                        }
                    )
                })
        }) {
        ScrollableColumn {
            AnimatedVisibility(visible = selectedDictionary != null) {
                ScrabblerForm(scrabblerViewModel, selectedDictionary)
            }
            if (selectedDictionary == null) {
                Text(
                    text = "Please select dictionary.",
                    style = MaterialTheme.typography.h1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Results(scrabblerViewModel)
        }
    }
}

@Composable
fun ScrabblerForm(scrabblerViewModel: ScrabblerViewModel, selectedDictionary: String?) {
    val keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search)

    val wordField = TextFormField("Word", savedInstanceState { "" }, keyboardOptions)
    val prefixField = TextFormField("Prefix", savedInstanceState { "" }, keyboardOptions)
    val containsField = TextFormField("Contains", savedInstanceState { "" }, keyboardOptions)
    val suffixField = TextFormField("Suffix", savedInstanceState { "" }, keyboardOptions)
    val regexFilterField = TextFormField("Filter (regex)", savedInstanceState { "" }, keyboardOptions)
    val useAllLettersField = BooleanFormField("Use all letters", savedInstanceState { true })
    val removeAccentsField = BooleanFormField("Remove accents", savedInstanceState { true })

    if (wordField.value.isEmpty()) {
        scrabblerViewModel.clearResults()
    }

    Surface(elevation = 5.dp) {
        Form(
            modifier = Modifier.padding(CONTENT_PADDING),
            fieldModifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            fields = listOf(
                wordField,
                prefixField,
                containsField,
                suffixField,
                regexFilterField,
                useAllLettersField,
                removeAccentsField,
            ),
            submitLabel = "Search",
            onSubmit = {
                if (selectedDictionary != null) {
                    scrabblerViewModel.onQueryChanged(
                        selectedDictionary,
                        ScrabblerQuery(
                            word = wordField.value,
                            prefix = prefixField.value,
                            suffix = suffixField.value,
                            contains = containsField.value,
                            regexFilter = regexFilterField.value.emptyToNull(),
                            useAllLetters = useAllLettersField.value,
                            removeAccents = removeAccentsField.value,
                        )
                    )
                }
            })
    }
}

@Composable
fun Results(scrabblerViewModel: ScrabblerViewModel, modifier: Modifier = Modifier) {
    val loadingState by scrabblerViewModel.loadingState.observeAsState()
    val results by scrabblerViewModel.results.observeAsState()
    if (loadingState == LoadingState.LOADING) {
        Box(modifier.fillMaxWidth().padding(CONTENT_PADDING), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (results != null) {
        Column(modifier.fillMaxWidth().padding(CONTENT_PADDING)) {
            Spacer(modifier = Modifier.preferredHeight(8.dp))
            if (results!!.isEmpty()) {
                Text(
                    "No results",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colors.error
                )
            }
            for (word in results!!) {
                Text(
                    word,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DictionarySelector(
    scaffoldState: ScaffoldState,
    selectedDictionary: String? = null,
    onDictionarySelected: (String?) -> Unit = {},
    onNewDictionarySelected: (String, String) -> Unit = { _, _ -> run {} },
    onDictionaryDeleted: (String) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var openDialog by remember { mutableStateOf(false) }
    var dictionaryPath: Uri? by remember { mutableStateOf(null) }
    var name by remember { mutableStateOf("") }

    val dictionaries by (AmbientContext.current.applicationContext as ScrabblerApplication).dictionaryDataService.dictionaries.collectAsState(
        listOf()
    )

    val contentResolver = AmbientContext.current.contentResolver
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
        onDismissRequest = { openDialog = false }
    ) {
        openDialog = false
        try {
            onNewDictionarySelected(it, dictionaryPath.toString())
        } catch (e: Exception) {
            Log.e("DictionarySelector", "Failed to load dictionary.", e)
            scope.launch { scaffoldState.snackbarHostState.showSnackbar("Failed to load dictionary.") }
        }
    }

    DropdownMenu(
        toggleModifier = Modifier.padding(CONTENT_PADDING),
        dropdownModifier = Modifier.fillMaxWidth(),
        toggle = {
            Button(onClick = { expanded = true }) {
                Icon(
                    modifier = Modifier.padding(end = 4.dp),
                    imageVector = vectorResource(id = R.drawable.ic_menu_book_black_18dp)
                )
                Text(selectedDictionary ?: "Select dictionary")
                Icon(Icons.Default.ArrowDropDown)
            }
        },
        expanded = expanded,
        onDismissRequest = { expanded = false }) {
        DropdownMenuItem(onClick = {
            expanded = false
            openFileLauncher.launch(
                arrayOf(
                    "text/plain",
                    "application/gzip",
                    "text/csv",
                    "text/comma-separated-values"
                )
            )
        }) {
            Text("Load from file.")
        }
        for (dictionary in dictionaries) {
            DropdownMenuItem(onClick = {
                expanded = false
                onDictionarySelected(dictionary.name)
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dictionary.name, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Delete, Modifier.clickable(onClick = {
                        expanded = false
                        onDictionaryDeleted(dictionary.name)
                    }).padding(4.dp))
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
@Preview(showBackground = true)
fun DefaultPreview() {
    ScrabblerTheme {
        ScrabblerApp(ScrabblerViewModel(ScrabblerApplication()))
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