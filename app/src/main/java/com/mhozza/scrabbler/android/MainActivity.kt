package com.mhozza.scrabbler.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.google.accompanist.insets.*
import com.mhozza.scrabbler.android.ui.ScrabblerTheme

@ExperimentalAnimationApi
class MainActivity : AppCompatActivity() {
    private val scrabblerViewModel by viewModels<ScrabblerViewModel> {
        AndroidViewModelFactory(
            application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        window.navigationBarColor = Color.Transparent.toArgb()
        window.statusBarColor = Color.Transparent.toArgb()
        setContent {
            ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
                ScrabblerTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colors.background, modifier = Modifier.fillMaxSize()) {
                        ScrabblerApp(scrabblerViewModel)
                    }
                }
            }
        }
    }
}
