package com.mhozza.scrabbler.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrabblerApp(scrabblerViewModel: ScrabblerViewModel = viewModel()) {
    val application = LocalContext.current.applicationContext as ScrabblerApplication
    val selectedDictionary by scrabblerViewModel.selectedDictionary.collectAsState()
    val isDictionaryLoading by scrabblerViewModel.isDictionaryLoading.collectAsState()
    val removeAccents by scrabblerViewModel.removeAccents.collectAsState()

    val navController = rememberNavController()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Scrabbler") },
                navigationIcon = {
                    // TODO: Hide when on homescreen.
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    DictionarySelector(
                        snackbarHostState,
                        selectedDictionary,
                        isDictionaryLoading = isDictionaryLoading,
                        onDictionarySelected = {
                            scrabblerViewModel.onSelectNewDictionary(it)
                        },
                        onNewDictionarySelected = { name, path ->
                            scrabblerViewModel.onLoadNewDictionary(name, path)
                            scrabblerViewModel.onSelectNewDictionary(name)
                        },
                        onDictionaryDeleted = {
                            if (it == selectedDictionary) {
                                scrabblerViewModel.onSelectNewDictionary(null)
                            }
                            with(application) {
                                applicationScope.launch {
                                    dictionaryDataService.deleteDictionary(it)
                                }
                            }
                        }
                    )
                })
        },
    ) { paddingValues ->
        NavHost(
            navController,
            startDestination = HomeScreenDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = CONTENT_PADDING)

        ) {
            composable<HomeScreenDestination> {
                HomeScreen(
                    selectedDictionary,
                    onNavigateToDestination = {
                        navController.navigate(it)
                    })
            }
            composable<PermutationsScreenDestination> {
                PermutationsScreen(
                    removeAccents,
                    scrabblerViewModel,
                )
            }
            composable<SearchScreenDestination> {
                SearchScreen(
                    removeAccents,
                    scrabblerViewModel,
                )
            }
        }
    }
}
