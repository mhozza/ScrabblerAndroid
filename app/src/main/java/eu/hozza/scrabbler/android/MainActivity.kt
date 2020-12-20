package eu.hozza.scrabbler.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.statusBarColor = Color(0xFFff6d00).toArgb()
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
