package eu.hozza.scrabbler.android

import android.content.ContentResolver
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.hozza.scrabbler.android.ui.ScrabblerTheme
import kotlinx.coroutines.launch
import java.nio.file.Paths

@Composable
fun ScrabblerApp(scrabblerViewModel: ScrabblerViewModel) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState, topBar = {
        TopAppBar(title = {Text("Scrabbler") })
    }) {
        ScrollableColumn(modifier = Modifier.padding(8.dp)) {
            DictionarySelector(scrabblerViewModel, scaffoldState)
            if (scrabblerViewModel.selectedDictionary.observeAsState().value != null) {
                Divider()
                ScrabblerForm(scrabblerViewModel)
            }
            if (!scrabblerViewModel.results.observeAsState().value.isNullOrEmpty()) {
                Divider()
                Results(scrabblerViewModel)
            }
        }
    }
}

@Composable
fun Divider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .preferredHeight(1.dp)
            .background(Color.Gray)
    )
}

@Composable
fun ScrabblerForm(scrabblerViewModel: ScrabblerViewModel) {
    val wordField = TextFormField("Word", savedInstanceState { "" })
    val prefixField = TextFormField("Prefix", savedInstanceState { "" })
    val wildcardField = BooleanFormField("Wildcard (?)", savedInstanceState { true })
    val allowShorterField = BooleanFormField("Allow shorter", savedInstanceState { false })

    Form(
        fieldModifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        fields = listOf(
            wordField,
            prefixField,
            wildcardField,
            allowShorterField,
        ),
        submitLabel = "Search",
        onSubmit = {
            scrabblerViewModel.onQueryChanged(
                ScrabblerQuery(
                    word = wordField.value,
                    wildcard = wildcardField.value,
                    prefix = prefixField.value,
                    allowShorter = allowShorterField.value,
                )
            )
        })
}

@Composable
fun Results(scrabblerViewModel: ScrabblerViewModel) {
    Column(Modifier.fillMaxWidth()) {
        Text("Results", style = MaterialTheme.typography.h3)
        Spacer(modifier = Modifier.preferredHeight(8.dp))
        val words: List<String> by scrabblerViewModel.results.observeAsState(listOf())
        for (word in words) {
            Text(
                word,
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.body1
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DictionarySelector(scrabblerViewModel: ScrabblerViewModel, scaffoldState: ScaffoldState) {
    var expanded by remember { mutableStateOf(false) }
    var openDialog by remember { mutableStateOf(false) }
    var dictionaryPath: Uri? by remember { mutableStateOf(null) }
    var name by remember { mutableStateOf("") }

    val dictionaries: List<DictionaryItem> by scrabblerViewModel.dictionaries.observeAsState(listOf())
    val selectedDictionary: String? by scrabblerViewModel.selectedDictionary.observeAsState(null)

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
            scrabblerViewModel.onLoadNewDictionary(it, dictionaryPath.toString())
        } catch (e: Exception) {
            Log.e("DictionarySelector", "Failed to load dictionary.", e)
            scope.launch { scaffoldState.snackbarHostState.showSnackbar("Failed to load dictionary.") }
        }
    }

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Dictionary: ")
        DropdownMenu(
            dropdownModifier = Modifier.weight(1.0f, fill = true),
            toggle = {
                Button(modifier = Modifier.fillMaxWidth(), onClick = { expanded = true }) {
                    Text(selectedDictionary ?: "Select...")
                    Icon(Icons.Default.ArrowDropDown)
                }
            },
            expanded = expanded,
            onDismissRequest = { expanded = false }) {
            DropdownMenuItem(onClick = {
                expanded = false
                openFileLauncher.launch(null)
            }) {
                Text("Load from file.")
            }
            for (dictionary in dictionaries) {
                DropdownMenuItem(onClick = {
                    expanded = false
                    scrabblerViewModel.onDictionarySelected(dictionary.name)
                }) {
                    Text(dictionary.name)
                }
            }
        }
    }
}

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
    val name = Paths.get(path).fileName
    return if (attempt == null) {
        name.toString()
    } else {
        "$name ($attempt)"
    }
}