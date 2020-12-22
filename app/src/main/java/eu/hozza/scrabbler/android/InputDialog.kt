package eu.hozza.scrabbler.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import eu.hozza.scrabbler.android.ui.ScrabblerTheme

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
                OutlinedTextField(
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
fun DialogPreview() {
    ScrabblerTheme {
        InputDialog(openDialog = true, onDismissRequest = { /*TODO*/ }, onConfirm = { /*TODO*/ })
    }
}