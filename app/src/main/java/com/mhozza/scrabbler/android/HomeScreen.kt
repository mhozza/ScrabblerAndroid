package com.mhozza.scrabbler.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhozza.scrabbler.android.ui.ScrabblerTheme
import kotlinx.serialization.Serializable

@Serializable
object HomeScreenDestination

private data class HomeItem(val label: String, val icon: ImageVector, val destination: Any, val requiresDictionary: Boolean = true)

@Composable fun HomeScreen(selectedDictionary: String?, modifier: Modifier = Modifier, onNavigateToDestination: (Any) -> Unit = {}) {
    val items = listOf(
        HomeItem("Permutations", Default.Refresh, PermutationsScreenDestination),
        HomeItem("Search", Default.Search, SearchScreenDestination),
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        items(items) {
            HomeMenuItemButton(it.label, it.icon, enabled = !it.requiresDictionary || selectedDictionary != null, onClick = {
                onNavigateToDestination(it.destination)
            })
        }
    }
}

@Preview
@Composable fun HomePreview() {
    ScrabblerTheme {
        HomeScreen("some disctionary")
    }
}

@Preview
@Composable fun HomePreviewNoDict() {
    ScrabblerTheme {
        HomeScreen(null)
    }
}

@Composable
fun HomeMenuItemButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, enabled:Boolean =  true, onClick: () -> Unit = {}) {
    Button(onClick = onClick, enabled = enabled) {
        Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(label)
        }
    }
}

@Preview
@Composable fun HomeMenuItemButtonPreview() {
    ScrabblerTheme {
        HomeMenuItemButton("Test", Default.Home)
    }
}
