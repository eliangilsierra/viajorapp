package app.viajor.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import app.viajor.ui.Navigator
import app.viajor.ui.ViajorApp

class MainActivity : ComponentActivity() {
    // Registers the file picker launchers, which must happen before the activity is started.
    private val files = AndroidFileAccess(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val dependencies = (application as ViajorApplication).dependencies
        setContent {
            // The back stack is not saved across process or configuration changes yet.
            val navigator = remember { Navigator() }
            BackHandler(enabled = navigator.canGoBack) { navigator.back() }
            ViajorApp(dependencies, files, navigator)
        }
    }
}
