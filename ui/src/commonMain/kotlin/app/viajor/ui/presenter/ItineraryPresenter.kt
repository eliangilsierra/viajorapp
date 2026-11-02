package app.viajor.ui.presenter

import app.viajor.data.ActivityDraft
import app.viajor.data.ActivityError
import app.viajor.data.ActivityRecord
import app.viajor.data.DayRecord
import app.viajor.data.ItineraryRepository
import app.viajor.data.PlaceRecord
import app.viajor.data.SaveActivityResult
import app.viajor.data.TripRecord
import app.viajor.data.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class DayWithActivities(val day: DayRecord, val activities: List<ActivityRecord>)

data class ItineraryState(
    val status: ListStatus = ListStatus.LOADING,
    val trip: TripRecord? = null,
    val days: List<DayWithActivities> = emptyList(),
    val unscheduled: List<ActivityRecord> = emptyList(),
    val places: List<PlaceRecord> = emptyList(),
    /** Problems from the last add or edit, for the form to show; cleared by [ItineraryPresenter.clearErrors]. */
    val errors: List<ActivityError> = emptyList(),
    /** Set right after a delete so the screen can offer Undo. */
    val undoableDelete: ActivityRecord? = null,
)

/** The day-by-day itinerary of one trip. No Android or Compose types. */
@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryPresenter(
    private val trips: TripRepository,
    private val itinerary: ItineraryRepository,
    private val scope: CoroutineScope,
    private val tripId: String,
) {
    private val _state = MutableStateFlow(ItineraryState())
    val state: StateFlow<ItineraryState> = _state.asStateFlow()

    init {
        val daysWithActivities: Flow<List<DayWithActivities>> = itinerary.observeDays(tripId).flatMapLatest { days ->
            if (days.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(days.map { day -> itinerary.observeActivities(day.id) }) { lists ->
                    days.mapIndexed { i, day -> DayWithActivities(day, lists[i]) }
                }
            }
        }
        scope.launch {
            combine(
                trips.observeTrip(tripId),
                daysWithActivities,
                itinerary.observeUnscheduled(tripId),
                itinerary.observePlaces(tripId),
            ) { trip, days, unscheduled, places ->
                _state.update {
                    it.copy(
                        status = if (trip == null) ListStatus.EMPTY else ListStatus.CONTENT,
                        trip = trip, days = days, unscheduled = unscheduled, places = places,
                    )
                }
            }
                .catch { _state.update { it.copy(status = ListStatus.ERROR) } }
                .collect { }
        }
    }

    fun addActivity(dayId: String?, draft: ActivityDraft) = launchGuarded {
        when (val r = itinerary.addActivity(tripId, dayId, draft)) {
            is SaveActivityResult.Invalid -> _state.update { it.copy(errors = r.errors) }
            is SaveActivityResult.Saved -> _state.update { it.copy(errors = emptyList()) }
            SaveActivityResult.NotFound -> _state.update { it.copy(status = ListStatus.EMPTY) }
        }
    }

    fun editActivity(id: String, draft: ActivityDraft) = launchGuarded {
        when (val r = itinerary.updateActivity(id, draft)) {
            is SaveActivityResult.Invalid -> _state.update { it.copy(errors = r.errors) }
            is SaveActivityResult.Saved -> _state.update { it.copy(errors = emptyList()) }
            SaveActivityResult.NotFound -> Unit
        }
    }

    fun move(activityId: String, toDayId: String?, toIndex: Int) = launchGuarded {
        val r = itinerary.moveActivity(activityId, toDayId, toIndex)
        if (r is SaveActivityResult.Invalid) _state.update { it.copy(errors = r.errors) }
    }

    fun setCompleted(activityId: String, completed: Boolean) = launchGuarded { itinerary.setCompleted(activityId, completed) }

    fun delete(activity: ActivityRecord) = launchGuarded {
        itinerary.deleteActivity(activity.id)
        _state.update { it.copy(undoableDelete = activity) }
    }

    fun undoDelete() {
        val activity = _state.value.undoableDelete ?: return
        _state.update { it.copy(undoableDelete = null) }
        launchGuarded { itinerary.restoreActivity(activity.id) }
    }

    fun dismissUndo() = _state.update { it.copy(undoableDelete = null) }

    /** Appends a day to an undated trip; dated trips change length through the trip editor. */
    fun addDay() = launchGuarded { trips.addDay(tripId) }

    fun clearErrors() = _state.update { it.copy(errors = emptyList()) }

    private fun launchGuarded(block: suspend () -> Unit): Job {
        return scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(status = ListStatus.ERROR) }
            }
        }
    }
}
