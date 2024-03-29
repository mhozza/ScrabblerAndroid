package com.mhozza.scrabbler.android

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import java.util.*

@Composable
fun <I, O> registerForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit
): ActivityResultLauncher<I> {
    // First, find the ActivityResultRegistry by casting the Context
    // (which is actually a ComponentActivity) to ActivityResultRegistryOwner
    val owner = LocalContext.current as ActivityResultRegistryOwner
    val activityResultRegistry = owner.activityResultRegistry

    // Keep track of the current onResult listener
    val currentOnResult = rememberUpdatedState(onResult)

    // It doesn't really matter what the key is, just that it is unique
    // and consistent across configuration changes
    val key = rememberSaveable { UUID.randomUUID().toString() }

    // TODO a working layer of indirection would be great
    val realLauncher = remember<ActivityResultLauncher<I>> {
        activityResultRegistry.register(key, contract) {
            currentOnResult.value(it)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            realLauncher.unregister()
        }
    }
    return realLauncher
}
