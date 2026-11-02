package app.viajor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.viajor.ui.presenter.ImportCandidate
import app.viajor.ui.presenter.ImportError
import app.viajor.ui.presenter.ImportPresenter
import app.viajor.ui.presenter.ImportStage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_back
import viajorapp.ui.generated.resources.action_choose_csv
import viajorapp.ui.generated.resources.action_import_n
import viajorapp.ui.generated.resources.action_review
import viajorapp.ui.generated.resources.day_heading
import viajorapp.ui.generated.resources.import_csv_hint
import viajorapp.ui.generated.resources.import_done
import viajorapp.ui.generated.resources.import_done_failed
import viajorapp.ui.generated.resources.import_error_bad_file
import viajorapp.ui.generated.resources.import_error_failed
import viajorapp.ui.generated.resources.import_error_nothing
import viajorapp.ui.generated.resources.import_error_table
import viajorapp.ui.generated.resources.import_paste_hint
import viajorapp.ui.generated.resources.import_review_title
import viajorapp.ui.generated.resources.import_title
import viajorapp.ui.generated.resources.import_uncertain
import viajorapp.ui.generated.resources.import_unparsed
import viajorapp.ui.generated.resources.move_to_ideas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(deps: AppDependencies, files: FileAccess, navigator: Navigator, tripId: String) {
    val scope = rememberCoroutineScope()
    val presenter = remember { ImportPresenter(deps.itinerary, scope, tripId, deps.timeZoneId) }
    val state by presenter.state.collectAsState()
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.import_title)) },
                navigationIcon = { TextButton(onClick = { navigator.back() }) { Text(stringResource(Res.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.error?.let { Text(it.message(), color = MaterialTheme.colorScheme.error) }
            when (state.stage) {
                ImportStage.INPUT -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(Res.string.import_paste_hint)) },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = { presenter.reviewText(text) }, enabled = text.isNotBlank()) { Text(stringResource(Res.string.action_review)) }
                    Text(stringResource(Res.string.import_csv_hint), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = {
                        scope.launch {
                            val picked = files.pickText(listOf("text/csv", "text/comma-separated-values", "text/plain", "application/csv", "application/octet-stream"))
                            if (picked != null) presenter.reviewCsv(picked)
                        }
                    }) { Text(stringResource(Res.string.action_choose_csv)) }
                }
                ImportStage.REVIEW -> {
                    Text(stringResource(Res.string.import_review_title), style = MaterialTheme.typography.titleMedium)
                    if (state.unparsedLines > 0) Text(stringResource(Res.string.import_unparsed, state.unparsedLines), style = MaterialTheme.typography.bodySmall)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(state.candidates, key = { it.id }) { c -> CandidateRow(c) { presenter.toggle(c.id) } }
                    }
                    val chosen = state.candidates.count { it.checked }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = presenter::confirm, enabled = chosen > 0) { Text(stringResource(Res.string.action_import_n, chosen)) }
                        OutlinedButton(onClick = presenter::back) { Text(stringResource(Res.string.action_back)) }
                    }
                }
                ImportStage.DONE -> {
                    Text(stringResource(Res.string.import_done, state.added), style = MaterialTheme.typography.titleMedium)
                    if (state.failed > 0) Text(stringResource(Res.string.import_done_failed, state.failed))
                    Button(onClick = { navigator.back() }) { Text(stringResource(Res.string.action_back)) }
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(c: ImportCandidate, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Checkbox(checked = c.checked, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(c.title, style = MaterialTheme.typography.bodyLarge)
            val where = c.dayIndex?.let { stringResource(Res.string.day_heading, it) } ?: stringResource(Res.string.move_to_ideas)
            val time = c.start?.let { s -> hhmm(s) + (c.end?.let { "–" + hhmm(it) } ?: "") }
            Text(listOfNotNull(where, time).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            if (c.uncertain) Text(stringResource(Res.string.import_uncertain), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun ImportError.message(): String = stringResource(
    when (this) {
        ImportError.NOTHING_FOUND -> Res.string.import_error_nothing
        ImportError.NOT_A_TABLE -> Res.string.import_error_table
        ImportError.BAD_FILE -> Res.string.import_error_bad_file
        ImportError.FAILED -> Res.string.import_error_failed
    },
)
