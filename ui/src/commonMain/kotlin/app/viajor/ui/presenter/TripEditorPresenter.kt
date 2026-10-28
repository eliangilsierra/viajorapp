package app.viajor.ui.presenter

import app.viajor.data.CreateTripResult
import app.viajor.data.TripRepository
import app.viajor.data.UpdateTripResult
import app.viajor.domain.TripError
import app.viajor.domain.TripInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.coroutines.cancellation.CancellationException

data class TripEditorState(
    val title: String = "",
    val description: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val errors: List<TripError> = emptyList(),
    /** Day numbers that hold activities and would be unscheduled by this change; the screen must ask first. */
    val daysNeedingConfirmation: List<Int> = emptyList(),
    val saving: Boolean = false,
    /** Set once saved; the screen navigates to this trip. */
    val savedTripId: String? = null,
    val failed: Boolean = false,
    val notFound: Boolean = false,
)

/** Create (tripId = null) or edit a trip. Validation and day changes follow the repository's rules. */
class TripEditorPresenter(
    private val repository: TripRepository,
    private val scope: CoroutineScope,
    private val tripId: String? = null,
) {
    private val _state = MutableStateFlow(TripEditorState())
    val state: StateFlow<TripEditorState> = _state.asStateFlow()

    init {
        if (tripId != null) {
            scope.launch {
                val trip = try {
                    repository.observeTrip(tripId).first()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.update { it.copy(failed = true) }
                    return@launch
                }
                if (trip == null) {
                    _state.update { it.copy(notFound = true) }
                } else {
                    _state.update {
                        it.copy(
                            title = trip.title, description = trip.description.orEmpty(),
                            startDate = trip.startDate, endDate = trip.endDate,
                        )
                    }
                }
            }
        }
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value, errors = it.errors - TripError.EMPTY_TITLE - TripError.TITLE_TOO_LONG) }

    fun setDescription(value: String) = _state.update { it.copy(description = value) }

    /** Both dates or neither; one date alone is reported as an error on save. */
    fun setDates(start: LocalDate?, end: LocalDate?) = _state.update {
        it.copy(startDate = start, endDate = end, errors = emptyList(), daysNeedingConfirmation = emptyList())
    }

    fun save(confirmDropDays: Boolean = false) {
        val current = _state.value
        if (current.saving) return
        _state.update { it.copy(saving = true, failed = false) }
        scope.launch {
            val input = TripInput(current.title, current.startDate, current.endDate)
            val description = current.description.ifBlank { null }
            try {
                if (tripId == null) {
                    when (val r = repository.createTrip(input, description)) {
                        is CreateTripResult.Created -> _state.update { it.copy(saving = false, savedTripId = r.tripId) }
                        is CreateTripResult.Invalid -> _state.update { it.copy(saving = false, errors = r.errors) }
                    }
                } else {
                    when (val r = repository.updateTrip(tripId, input, description, confirmDropDays)) {
                        UpdateTripResult.Updated -> _state.update { it.copy(saving = false, savedTripId = tripId, daysNeedingConfirmation = emptyList()) }
                        UpdateTripResult.NotFound -> _state.update { it.copy(saving = false, notFound = true) }
                        is UpdateTripResult.Invalid -> _state.update { it.copy(saving = false, errors = r.errors) }
                        is UpdateTripResult.NeedsConfirmation -> _state.update { it.copy(saving = false, daysNeedingConfirmation = r.dayIndexes) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, failed = true) }
            }
        }
    }
}
