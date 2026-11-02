package app.viajor.ui.presenter

import app.viajor.data.TripRecord
import app.viajor.data.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

enum class ListStatus { LOADING, EMPTY, CONTENT, ERROR }

/** What the trip list screen shows. [undoableDelete] is set right after a delete so the screen can offer Undo. */
data class TripListState(
    val status: ListStatus = ListStatus.LOADING,
    val trips: List<TripRecord> = emptyList(),
    val undoableDelete: TripRecord? = null,
)

/**
 * State holder for the trip list: no Android or Compose types, so it is tested on the JVM. A repository
 * failure becomes [ListStatus.ERROR]; it never crashes the screen.
 */
class TripListPresenter(private val repository: TripRepository, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(TripListState())
    val state: StateFlow<TripListState> = _state.asStateFlow()

    init {
        scope.launch {
            repository.observeTrips()
                .catch { _state.update { it.copy(status = ListStatus.ERROR) } }
                .collect { trips ->
                    _state.update { it.copy(status = if (trips.isEmpty()) ListStatus.EMPTY else ListStatus.CONTENT, trips = trips) }
                }
        }
    }

    fun delete(trip: TripRecord) {
        scope.launch {
            if (guarded { repository.deleteTrip(trip.id) }) _state.update { it.copy(undoableDelete = trip) }
        }
    }

    fun undoDelete() {
        val trip = _state.value.undoableDelete ?: return
        _state.update { it.copy(undoableDelete = null) }
        scope.launch { guarded { repository.restoreTrip(trip.id) } }
    }

    fun dismissUndo() = _state.update { it.copy(undoableDelete = null) }

    fun archive(trip: TripRecord) {
        scope.launch { guarded { repository.setArchived(trip.id, true) } }
    }

    private suspend fun guarded(block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        _state.update { it.copy(status = ListStatus.ERROR) }
        false
    }
}
