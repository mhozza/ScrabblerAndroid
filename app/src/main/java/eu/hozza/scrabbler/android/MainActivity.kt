package eu.hozza.scrabbler.android

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import eu.hozza.scrabbler.android.ui.ScrabblerTheme

class MainActivity : AppCompatActivity() {
    private val scrabblerViewModel by viewModels<ScrabblerViewModel> {
        AndroidViewModelFactory(
            application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScrabblerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    ScrabblerApp(scrabblerViewModel)
                }
            }
        }
    }
}
