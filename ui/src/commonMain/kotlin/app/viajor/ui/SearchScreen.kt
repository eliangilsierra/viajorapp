package app.viajor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.viajor.data.SearchKind
import app.viajor.ui.presenter.SearchPresenter
import app.viajor.ui.presenter.SearchStatus
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_back
import viajorapp.ui.generated.resources.action_search
import viajorapp.ui.generated.resources.load_error
import viajorapp.ui.generated.resources.search_empty
import viajorapp.ui.generated.resources.search_hint
import viajorapp.ui.generated.resources.search_kind_activity
import viajorapp.ui.generated.resources.search_kind_place
import viajorapp.ui.generated.resources.search_start

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(deps: AppDependencies, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    val presenter = remember { SearchPresenter(deps.search, scope) }
    val state by presenter.state.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.action_search)) })
        },
        bottomBar = { MainNavigationBar(Screen.Search, navigator) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = presenter::setQuery,
                label = { Text(stringResource(Res.string.search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            when (state.status) {
                SearchStatus.IDLE -> Text(stringResource(Res.string.search_start), style = MaterialTheme.typography.bodyMedium)
                SearchStatus.EMPTY -> Text(stringResource(Res.string.search_empty), style = MaterialTheme.typography.bodyMedium)
                SearchStatus.ERROR -> Text(stringResource(Res.string.load_error), color = MaterialTheme.colorScheme.error)
                SearchStatus.RESULTS -> LazyColumn(contentPadding = PaddingValues(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.hits, key = { it.kind.name + it.id }) { hit ->
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable {
                                // Replace the search with the trip so Back returns to the list.
                                navigator.replace(Screen.Itinerary(hit.tripId))
                            }.padding(vertical = 8.dp),
                        ) {
                            Text(
                                stringResource(if (hit.kind == SearchKind.PLACE) Res.string.search_kind_place else Res.string.search_kind_activity),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(hit.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
