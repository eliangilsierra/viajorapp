package app.viajor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.viajor.designsystem.ViajorTheme
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.app_name
import viajorapp.ui.generated.resources.empty_trips_body
import viajorapp.ui.generated.resources.empty_trips_title
import viajorapp.ui.generated.resources.tagline

/** Application root shared by Android and iOS. The create-trip action arrives with the trips feature (0.2); no placeholder button is shown. */
@Composable
fun ViajorApp() {
    ViajorTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(Res.string.tagline), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(Res.string.empty_trips_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.empty_trips_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
