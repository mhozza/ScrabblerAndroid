package com.mhozza.scrabbler.android

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mhozza.scrabbler.android.ui.ScrabblerTheme

@Composable
fun InputDialog(
    initialValue: String = "",
    openDialog: Boolean,
    validator: (String) -> Boolean = {true},
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
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
        Dialog(onDismissRequest = {
            onDismissRequest()
        }) {
            Surface(elevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                val requester = FocusRequester()
                onCommit { requester.requestFocus() }
                Column {
                    OutlinedTextField(
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .focusRequester(requester),
                        value = value,
                        onValueChange = { value = it },
                        onTextInputStarted = {
                            it.showSoftwareKeyboard()
                        },
                        isErrorValue = !validator(value.text)
                    )
                    Row {
                        Button(
                            modifier = Modifier.weight(1f).padding(4.dp),
                            enabled = validator(value.text),
                            onClick = {
                                onConfirm(value.text)
                            }) {
                            Text("OK")
                        }
                        Button(
                            modifier = Modifier.weight(1f).padding(4.dp),
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
    }
}


@Composable
@Preview(showBackground = true)
fun DialogPreview() {
    ScrabblerTheme {
        InputDialog(openDialog = true, onDismissRequest = { /*TODO*/ }, onConfirm = { /*TODO*/ })
    }
}