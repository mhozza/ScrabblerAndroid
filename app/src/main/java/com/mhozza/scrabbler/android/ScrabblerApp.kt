package com.mhozza.scrabbler.android

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
        icon = { Icon(Default.Refresh, contentDescription = null) },
        label = { Text("Permutations") }
    ),
    SEARCH(
        icon = { Icon(Default.Search, contentDescription = null) },
        label = { Text("Search") }
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrabblerApp(scrabblerViewModel: ScrabblerViewModel = viewModel()) {
    val application = LocalContext.current.applicationContext as ScrabblerApplication
    val selectedDictionary by application.settingsDataService.selectedDictionary.get().map {
        if (it == null || application.dictionaryDataService.getDictionaryUri(it) == null)
            null
        else
            it
    }
        .collectAsState(null)
    val selectedSearchMode by application.settingsDataService.selectedMode.get().collectAsState(
        initial = SearchMode.PERMUTATIONS
    )

    fun setSelectedDictionary(dictionary: String?) {
        application.applicationScope.launch {
            application.settingsDataService.selectedDictionary.set(dictionary)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Scrabbler") },
                actions = {
                    DictionarySelector(
                        snackbarHostState,
                        selectedDictionary,
                        onDictionarySelected = {
                            setSelectedDictionary(it)
                        },
                        onNewDictionarySelected = { name, path ->
                            scrabblerViewModel.onLoadNewDictionary(name, path)
                            setSelectedDictionary(name)
                        },
                        onDictionaryDeleted = {
                            if (it == selectedDictionary) {
                                setSelectedDictionary(null)
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
            AnimatedVisibility(
                visible = selectedDictionary != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            ) {
                NavigationBar {
                    for (mode in enumValues<SearchMode>()) {
                        NavigationBarItem(
                            selected = mode == selectedSearchMode,
                            onClick = {
                                application.applicationScope.launch {
                                    application.settingsDataService.selectedMode.set(mode)
                                }
                            },
                            icon = mode.icon,
                            label = mode.label,
                        )
                    }
                }
            }
        }
    ) {
        val resultsState by scrabblerViewModel.resultsState.collectAsState()

        if (selectedDictionary == null) {
            SelectDictionaryPrompt(
                Modifier
                    .fillMaxSize()
                    .padding(it)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                if (selectedSearchMode == SearchMode.PERMUTATIONS) {
                    PermutationsForm(onQueryChanged = { query ->
                        scrabblerViewModel.onQueryChanged(
                            selectedDictionary!!,
                            query,
                        )
                    },
                    onClearResults = { scrabblerViewModel.clearResults() })
                } else {
                    SearchForm(onQueryChanged = { query ->
                        scrabblerViewModel.onQueryChanged(
                            selectedDictionary!!,
                            query,
                        )
                    },
                        onClearResults = { scrabblerViewModel.clearResults() })
                }
                Spacer(modifier = Modifier.height(1.dp).background(MaterialTheme.colorScheme.outline))
                Results(resultsState)
            }
        }
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
fun PermutationsForm(modifier: Modifier = Modifier,
                     onQueryChanged: (ScrabblerQuery) -> Unit = {},
                     onClearResults: () -> Unit = {},) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface).animateContentSize()) {
        val keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search)

        var collapsed by rememberSaveable { mutableStateOf(false) }

        var wordFieldState by rememberSaveable { mutableStateOf("") }
        var prefixFieldState by rememberSaveable { mutableStateOf("") }
        var containsField by rememberSaveable { mutableStateOf("") }
        var suffixFieldState by rememberSaveable { mutableStateOf("") }
        var regexFilterField by rememberSaveable { mutableStateOf("") }
        var useAllLettersField by rememberSaveable { mutableStateOf(true) }
        var removeAccentsField by rememberSaveable { mutableStateOf(true) }

        SideEffect {
            if(wordFieldState.isEmpty()) {
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
                    removeAccents = removeAccentsField,
                )
            onQueryChanged(query)
            collapsed = true
        }

        val defaultFieldModifier = Modifier.fillMaxWidth()

        if (collapsed) {
            Row(defaultFieldModifier, verticalAlignment = Alignment.CenterVertically) {
                TextFormWidget(
                    wordFieldState,
                    onValueChange = { wordFieldState = it },
                    modifier = Modifier.weight(1f),
                    label = "Word",
                    keyboardOptions = keyboardOptions,
                    onClick = {collapsed = false},
                    onSubmit = onSubmit
                )
                IconButton(onClick = { collapsed = false }) {
                    Icon(
                        Default.ArrowDropDown,
                        "Expand form."
                    )
                }
            }
        } else {
            TextFormWidget(wordFieldState, onValueChange = {wordFieldState = it}, modifier = defaultFieldModifier, label = "Word", keyboardOptions = keyboardOptions, onSubmit = onSubmit)
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
                removeAccentsField,
                onValueChange = { removeAccentsField = it },
                modifier = defaultFieldModifier,
                label = "Remove accents"
            )
        }

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


@Composable
fun SearchForm(modifier: Modifier = Modifier,
                     onQueryChanged: (ScrabblerQuery) -> Unit = {},
                     onClearResults: () -> Unit = {},) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        val keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Search)

        var wordFieldState by rememberSaveable { mutableStateOf("") }

        SideEffect {
            if(wordFieldState.isEmpty()) {
                onClearResults()
            }
        }

        var removeAccentsField by rememberSaveable { mutableStateOf(true) }

        val defaultFieldModifer = Modifier.fillMaxWidth()

        TextFormWidget(wordFieldState, onValueChange = {wordFieldState = it}, modifier = defaultFieldModifer, label = "Word", keyboardOptions = keyboardOptions)
        BooleanFormWidget(removeAccentsField, onValueChange = {removeAccentsField = it}, modifier = defaultFieldModifer, label = "Remove accents")

        Button(
            modifier = defaultFieldModifer,
            onClick = {
                val query =
                    SearchScrabblerQuery(
                        word = wordFieldState,
                        removeAccents = removeAccentsField,
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
fun PermutationsFormPreview() {
    ScrabblerTheme {
        PermutationsForm()
    }
}


@Preview
@Composable
fun SearchFormPreview() {
    ScrabblerTheme {
        SearchForm()
    }
}

@Composable
fun Results(resultsState: ResultsState, modifier: Modifier = Modifier) {
    when (resultsState) {
        is ResultsState.Loading -> Box(
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(CONTENT_PADDING), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        is ResultsState.Loaded -> {
            Column(
                modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(CONTENT_PADDING)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                if (resultsState.results.isEmpty()) {
                    Text(
                        "No results",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                for (word in resultsState.results) {
                    Text(
                        word,
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        is ResultsState.Idle -> Box(
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(CONTENT_PADDING), contentAlignment = Alignment.Center
        ) {
            Text(
                "Please type a query",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
fun DictionarySelector(
    snackbarHostState: SnackbarHostState,
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
            scope.launch { snackbarHostState.showSnackbar("Failed to load dictionary.") }
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