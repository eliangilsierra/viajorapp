package app.viajor.ui.presenter

import app.viajor.data.SearchHit
import app.viajor.data.SearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

enum class SearchStatus { IDLE, RESULTS, EMPTY, ERROR }

data class SearchState(val query: String = "", val status: SearchStatus = SearchStatus.IDLE, val hits: List<SearchHit> = emptyList())

/** Search over places and activities. An older search is cancelled when the text changes. */
class SearchPresenter(private val repository: SearchRepository, private val scope: CoroutineScope, private val tripId: String? = null) {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    private var running: Job? = null

    fun setQuery(text: String) {
        _state.update { it.copy(query = text) }
        running?.cancel()
        if (text.isBlank()) {
            _state.update { it.copy(status = SearchStatus.IDLE, hits = emptyList()) }
            return
        }
        running = scope.launch {
            try {
                val hits = repository.search(text, tripId)
                _state.update { it.copy(status = if (hits.isEmpty()) SearchStatus.EMPTY else SearchStatus.RESULTS, hits = hits) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(status = SearchStatus.ERROR, hits = emptyList()) }
            }
        }
    }
}
