package com.mhozza.scrabbler.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview

abstract class FormField<T>(
    private val state: MutableState<T>,
    private val _widget: @Composable (Modifier, () -> Unit) -> Unit
) {
    val value: T
        get() = state.value

    val widget: @Composable (Modifier, () -> Unit) -> Unit = { modifier, onSubmit ->
        _widget.invoke(modifier, onSubmit)
    }
}

class TextFormField @OptIn(ExperimentalComposeUiApi::class) constructor(
    label: String = "",
    state: MutableState<String>,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    widget: @Composable ((Modifier, () -> Unit) -> Unit) = { modifier, onSubmit ->
        val keyboardController = LocalSoftwareKeyboardController.current
        OutlinedTextField(
            value = state.value,
            onValueChange = { state.value = it },
            modifier = modifier,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                onSubmit()
            })
        )
    }
) : FormField<String>(state, widget)

class BooleanFormField(
    label: String = "",
    state: MutableState<Boolean>,
    widget: @Composable ((Modifier, () -> Unit) -> Unit) = { modifier, _ ->
        LabeledCheckbox(
            modifier = modifier,
            label = label,
            value = state.value,
            onValueChange = { state.value = it }
        )
    }
) : FormField<Boolean>(state, widget)

@Composable
fun LabeledCheckbox(
    modifier: Modifier,
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Row(modifier = modifier.clickable(onClick = { onValueChange(!value) })) {
        Checkbox(checked = value, onCheckedChange = onValueChange)
        Text(label)
    }
}

@Composable
fun Form(
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    fields: List<FormField<*>>,
    submitLabel: String = "Submit",
    onSubmit: () -> Unit = {}
) {
    Column(modifier) {
        for (field in fields) {
            field.widget(fieldModifier, onSubmit)
        }
        Button(
            modifier = fieldModifier,
            onClick = onSubmit
        ) {
            Text(submitLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FormExample() {
    val fields = listOf(
        TextFormField("Foo", remember { mutableStateOf("") }),
        TextFormField("Bar", remember { mutableStateOf("initial value") }),
        BooleanFormField("Checkbox", remember { mutableStateOf(false) })
    )
    Form(fields = fields)
}