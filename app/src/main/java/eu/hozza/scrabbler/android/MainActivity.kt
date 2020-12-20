package eu.hozza.scrabbler.android

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.*
import eu.hozza.scrabbler.android.ui.ScrabblerTheme
import kotlinx.coroutines.launch
import java.nio.file.Paths

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScrabblerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    ScrabblerApp(application as ScrabblerApplication)
                }
            }
        }
    }
}

class ScrabblerViewModel(private val application: ScrabblerApplication) : ViewModel() {

    private val _results = MutableLiveData(listOf<String>())
    val results: LiveData<List<String>> = _results

    val dictionaries: LiveData<List<DictionaryItem>> =
        application.dictionaryDataService.dictionaries.asLiveData()
    private val _selectedDictionary: MutableLiveData<String> =
        MutableLiveData(application.dictionaryDataService.currentDictionaryName)
    val selectedDictionary: LiveData<String> = _selectedDictionary


    fun onQueryChanged(newQuery: ScrabblerQuery) {
        viewModelScope.launch {
            _results.value = application.scrabblerDataService.findPermutations(newQuery)
        }
    }

    fun onDictionarySelected(dictionaryName: String) {
        viewModelScope.launch {
            _selectedDictionary.value =
                application.dictionaryDataService.selectDictionary(dictionaryName)
        }
    }

    fun onLoadNewDictionary(name: String, fname: String) {
        viewModelScope.launch {
            _selectedDictionary.value =
                application.dictionaryDataService.loadDictionary(name, fname)
        }
    }
}

@Composable
fun ScrabblerApp(application: ScrabblerApplication) {
    val scaffoldState = rememberScaffoldState()
    val scrabblerViewModel = ScrabblerViewModel(application)
    Scaffold(scaffoldState = scaffoldState) {
        Column(modifier = Modifier.padding(8.dp)) {
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
    var word by savedInstanceState { "" }
    var prefix by savedInstanceState { "" }
    var wildcard by savedInstanceState { false }

    val fields = listOf(
        TextFormField("Word", word, { word = it }),
        BooleanFormField("Wildcard", wildcard, { wildcard = it }),
    )

    Form(
        fieldModifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        fields = fields,
        submitLabel = "Search",
        onSubmit = { scrabblerViewModel.onQueryChanged(ScrabblerQuery(word)) })
}

@Composable
fun Results(scrabblerViewModel: ScrabblerViewModel) {
    Column() {
        Text("Results", style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.preferredHeight(8.dp))
        val words: List<String> by scrabblerViewModel.results.observeAsState(listOf())
        for (word in words) {
            Text(word, style = MaterialTheme.typography.body1)
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
            scope.launch { scaffoldState.snackbarHostState.showSnackbar(e.localizedMessage) }
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

@OptIn(ExperimentalFocus::class)
@Composable
fun InputDialog(
    initialValue: String = "",
    openDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (openDialog) {
        var value by remember {
            mutableStateOf(
                TextFieldValue(
                    initialValue,
                    selection = TextRange(0, initialValue.length)
                )
            )
        }
        Dialog(onDismissRequest = onDismissRequest) {
            val requester = FocusRequester()
            onActive(callback = { requester.requestFocus() })

            Column(
                Modifier
                    .background(color = MaterialTheme.colors.background)
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .focusRequester(requester),
                    value = value,
                    onValueChange = { value = it },
                    onTextInputStarted = {
                        it.showSoftwareKeyboard()
                    }
                )
                Row() {
                    Button(
                        modifier = Modifier.weight(1f).padding(4.dp),
                        onClick = { onConfirm(value.text) }) {
                        Text("OK")
                    }
                    Button(
                        modifier = Modifier.weight(1f).padding(4.dp),
                        onClick = onDismissRequest
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun DefaultPreview() {
    ScrabblerTheme {
        ScrabblerApp(ScrabblerApplication())
    }
}

@Composable
@Preview(showBackground = true)
fun DialogPreview() {
    ScrabblerTheme {
        InputDialog(openDialog = true, onDismissRequest = { /*TODO*/ }, onConfirm = { /*TODO*/ })
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