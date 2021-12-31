import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import com.mhozza.scrabbler.android.SearchMode

@Composable
fun AddDictionaryScreen(navController: NavController, uri: Uri, defaultName: String) {
//    Text(uri.toString())
//    Text(defaultName)

    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                contentPadding = rememberInsetsPaddingValues(
                    insets = LocalWindowInsets.current.statusBars,
                    applyBottom = false,
                ),
                backgroundColor = MaterialTheme.colors.primary,
                title = { Text("Load dictionary") },)
        },
    ) {
//    val validator = { !dictionaries.contains(it) }

        val onConfirm = { _: String -> TODO() }
        val onDismissRequest = { TODO() }

        InputForm(
            defaultName,
            modifier = Modifier.padding(it),
            onDismissRequest = onDismissRequest,
            onConfirm = onConfirm
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputForm(initialValue: String = "",
              modifier: Modifier,
              validator: (String) -> Boolean = { true },
              onDismissRequest: () -> Unit,
              onConfirm: (String) -> Unit,
) {
    var value by remember {
        mutableStateOf(
            TextFieldValue(
                initialValue,
                selection = TextRange(0, initialValue.length)
            )
        )
    }

    Surface(elevation = 8.dp, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Column {
            val requester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

            OutlinedTextField(
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .focusRequester(requester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            keyboardController?.show()
                        }
                    },
                value = value,
                onValueChange = { value = it },
                isError = !validator(value.text)
            )
            SideEffect {
                requester.requestFocus()
            }
            Row {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    enabled = validator(value.text),
                    onClick = {
                        onConfirm(value.text)
                    }) {
                    Text("OK")
                }
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
