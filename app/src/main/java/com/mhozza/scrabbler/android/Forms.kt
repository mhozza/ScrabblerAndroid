package com.mhozza.scrabbler.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhozza.scrabbler.android.ui.ScrabblerTheme

@Composable fun TextFormWidget(
     value: String,
     onValueChange: (String) -> Unit,
     modifier: Modifier = Modifier,
     label: String = "",
     keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
     onClick: (() -> Unit)? = null,
     onSubmit: () -> Unit = {})  {
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }

    if(onClick != null) {
        LaunchedEffect(onClick, interactionSource) {
            interactionSource.interactions.collect {
                if(it is PressInteraction.Release) {
                    onClick()
                }
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        interactionSource = interactionSource,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onSearch = {
            keyboardController?.hide()
            onSubmit()
        })
    )
}

@Preview
@Composable
fun TextFormWidgetPreview() {
    ScrabblerTheme {
        TextFormWidget("bar", {}, modifier = Modifier.width(200.dp), label = "foo")
    }
}


@Composable
fun BooleanFormWidget(value: Boolean, onValueChange: (Boolean) -> Unit, modifier: Modifier = Modifier, label: String = "") {
    LabeledCheckbox(
        modifier = modifier,
        label = label,
        value = value,
        onValueChange = onValueChange
    )
}

@Preview
@Composable
fun BooleanFormWidgetPreview() {
    ScrabblerTheme {
        BooleanFormWidget(true, {}, modifier = Modifier.width(200.dp), label = "foo")
    }
}

@Composable
fun LabeledCheckbox(
    modifier: Modifier,
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Row(modifier = modifier.clickable(onClick = { onValueChange(!value) }), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = value, onCheckedChange = onValueChange)
        Text(label)
    }
}
