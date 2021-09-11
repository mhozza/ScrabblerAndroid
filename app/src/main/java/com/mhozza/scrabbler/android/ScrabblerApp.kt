package com.mhozza.scrabbler.android

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhozza.scrabbler.android.ui.ScrabblerTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.nio.file.Paths

private val CONTENT_PADDING = 8.dp

enum class SearchMode(
    val icon: @Composable () -> Unit = {},
    val label: @Composable () -> Unit = {},
) {
    PERMUTATIONS(
        icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
        label = { Text("Permutations") }
    ),
    SEARCH(
        icon = { Icon(Icons.Default.Search, contentDescription = null) },
        label = { Text("Search") }
    ),
}

@ExperimentalAnimationApi
@Composable
fun ScrabblerApp(scrabblerViewModel: ScrabblerViewModel) {
    val scaffoldState = rememberScaffoldState()
    var selectedDictionary: String? by rememberSaveable { mutableStateOf(null) }
    val application = LocalContext.current.applicationContext as ScrabblerApplication
    var selectedSearchMode by remember {
        mutableStateOf(SearchMode.PERMUTATIONS)
    }

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
        },
        bottomBar = {
            AnimatedVisibility(visible = selectedDictionary != null) {
                BottomNavigation {
                    for (mode in enumValues<SearchMode>()) {
                        BottomNavigationItem(
                            selected = mode == selectedSearchMode,
                            onClick = { selectedSearchMode = mode },
                            icon = mode.icon,
                            label = mode.label,
                        )
                    }
                }
            }
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            item {
                AnimatedVisibility(visible = selectedDictionary != null) {
                    ScrabblerForm(scrabblerViewModel, selectedDictionary, selectedSearchMode)
                }
            }
            if (selectedDictionary == null) {
                item {
                    Text(
                        text = "Please select dictionary.",
                        style = MaterialTheme.typography.h1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Results(scrabblerViewModel)
            }
        }
    }
}

@Composable
fun ScrabblerForm(
    scrabblerViewModel: ScrabblerViewModel,
    selectedDictionary: String?,
    searchMode: SearchMode
) {
    val keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search)

    val wordField = TextFormField("Word", rememberSaveable { mutableStateOf("") }, keyboardOptions)
    val prefixField =
        TextFormField("Prefix", rememberSaveable { mutableStateOf("") }, keyboardOptions)
    val containsField =
        TextFormField("Contains", rememberSaveable { mutableStateOf("") }, keyboardOptions)
    val suffixField =
        TextFormField("Suffix", rememberSaveable { mutableStateOf("") }, keyboardOptions)
    val regexFilterField =
        TextFormField("Filter (regex)", rememberSaveable { mutableStateOf("") }, keyboardOptions)
    val useAllLettersField =
        BooleanFormField("Use all letters", rememberSaveable { mutableStateOf(true) })
    val removeAccentsField =
        BooleanFormField("Remove accents", rememberSaveable { mutableStateOf(true) })

    if (wordField.value.isEmpty()) {
        scrabblerViewModel.clearResults()
    }

    val permutationSearchFields = listOf(
        wordField,
        prefixField,
        containsField,
        suffixField,
        regexFilterField,
        useAllLettersField,
        removeAccentsField,
    )

    val simpleSearchFields = listOf(
        wordField,
        removeAccentsField,
    )

    Surface(elevation = 5.dp) {
        Form(
            modifier = Modifier.padding(CONTENT_PADDING),
            fieldModifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            fields = if (searchMode == SearchMode.PERMUTATIONS) permutationSearchFields else simpleSearchFields,
            submitLabel = "Search",
            onSubmit = {
                if (selectedDictionary != null) {
                    val query = if (searchMode == SearchMode.PERMUTATIONS) {
                        PermutationsScrabblerQuery(
                            word = wordField.value,
                            prefix = prefixField.value,
                            suffix = suffixField.value,
                            contains = containsField.value,
                            regexFilter = regexFilterField.value.emptyToNull(),
                            useAllLetters = useAllLettersField.value,
                            removeAccents = removeAccentsField.value,
                        )
                    } else {
                        SearchScrabblerQuery(
                            word = wordField.value,
                            removeAccents = removeAccentsField.value,
                        )
                    }
                    scrabblerViewModel.onQueryChanged(
                        selectedDictionary,
                        query,
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
        Box(
            modifier
                .fillMaxWidth()
                .padding(CONTENT_PADDING), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (results != null) {
        Column(
            modifier
                .fillMaxWidth()
                .padding(CONTENT_PADDING)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
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
            scope.launch { scaffoldState.snackbarHostState.showSnackbar("Failed to load dictionary.") }
        }
    }

    Box(modifier = Modifier.padding(CONTENT_PADDING)) {
        Button(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu_book_black_18dp),
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(selectedDictionary ?: "Select dictionary")
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(
            modifier = Modifier.fillMaxWidth(),
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
                    onDictionarySelected(dictionary)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dictionary, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.Delete, contentDescription = "Delete",
                            Modifier
                                .clickable(onClick = {
                                    expanded = false
                                    onDictionaryDeleted(dictionary)
                                })
                                .padding(4.dp)
                        )
                    }
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