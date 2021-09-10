package com.mhozza.scrabbler.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.common.flogger.FluentLogger
import com.mhozza.scrabbler.android.ui.ScrabblerTheme

val logger: FluentLogger = FluentLogger.forEnclosingClass()

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputDialog(
    initialValue: String = "",
    openDialog: Boolean,
    validator: (String) -> Boolean = { true },
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
                Column {
                    val requester = remember { FocusRequester() }
                    val keyboardController = LocalSoftwareKeyboardController.current

                    OutlinedTextField(
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .focusTarget()
                            .focusRequester(requester),
                        value = value,
                        onValueChange = { value = it },
                        isError = !validator(value.text)
                    )
                    LaunchedEffect(Unit) {
                        logger.atFine().log("Requesting focus.\n%s\n%s", requester, keyboardController)
                        requester.requestFocus()
                        keyboardController?.show()
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
    }
}


@Composable
@Preview(showBackground = true)
fun DialogPreview() {
    ScrabblerTheme {
        InputDialog(openDialog = true, onDismissRequest = { /*TODO*/ }, onConfirm = { /*TODO*/ })
    }
}