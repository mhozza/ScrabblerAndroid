package eu.hozza.scrabbler.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

interface FormField {
    val widget: @Composable (Modifier) -> Unit
}

class TextFormField(
    label: String = "",
    value: String = "",
    onValueChange: (String) -> Unit = {},
    widget: (() -> Unit)? = null
) : FormField {
    override val widget: @Composable (Modifier) -> Unit = { modifier: Modifier ->
        widget ?: OutlinedTextField(
            modifier = modifier,
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) })
    }
}

class BooleanFormField(
    label: String = "",
    value: Boolean = false,
    onValueChange: (Boolean) -> Unit = {},
    widget: (() -> Unit)? = null
) : FormField {
    override val widget: @Composable (Modifier) -> Unit = { modifier: Modifier ->
        widget ?: LabeledCheckbox(
            modifier = modifier,
            label = label,
            value = value,
            onValueChange = onValueChange
        )
    }
}

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
    fields: List<FormField>,
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
    val fields = listOf(TextFormField("Foo"), TextFormField("Bar"), BooleanFormField("Checkbox"))
    Form(fields = fields)
}