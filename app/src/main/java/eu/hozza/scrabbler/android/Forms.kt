package eu.hozza.scrabbler.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

abstract class FormField<T>(
    label: String,
    private val state: MutableState<T>,
    widget: @Composable ((Modifier, String, MutableState<T>) -> Unit)
) {
    val value: T
        get() = state.value

    val widget: @Composable (Modifier) -> Unit = { modifier: Modifier ->
        widget.invoke(modifier, label, state)
    }
}

class TextFormField(
    label: String = "",
    state: MutableState<String>,
    widget: @Composable ((Modifier, String, MutableState<String>) -> Unit) = { modifier, label, state ->
        OutlinedTextField(
            modifier = modifier,
            value = state.value,
            onValueChange = { state.value = it },
            label = { Text(label) })
    }
) : FormField<String>(label, state, widget) {

}

class BooleanFormField(
    label: String = "",
    state: MutableState<Boolean>,
    widget: @Composable ((Modifier, String, MutableState<Boolean>) -> Unit) = { modifier, label, state ->
        LabeledCheckbox(
            modifier = modifier,
            label = label,
            value = state.value,
            onValueChange = { state.value = it }
        )
    }
) : FormField<Boolean>(label, state, widget)

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
            field.widget(fieldModifier)
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
        TextFormField("Foo", mutableStateOf("")),
        TextFormField("Bar", mutableStateOf("initial value")),
        BooleanFormField("Checkbox", mutableStateOf(false))
    )
    Form(fields = fields)
}