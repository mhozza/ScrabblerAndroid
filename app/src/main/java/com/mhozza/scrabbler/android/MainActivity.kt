package com.mhozza.scrabbler.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.mhozza.scrabbler.android.ui.ScrabblerTheme

@ExperimentalAnimationApi
class MainActivity : AppCompatActivity() {
    private val scrabblerViewModel by viewModels<ScrabblerViewModel> {
        AndroidViewModelFactory(
            application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ScrabblerTheme {
                ScrabblerApp(scrabblerViewModel)
            }
        }
    }
}
