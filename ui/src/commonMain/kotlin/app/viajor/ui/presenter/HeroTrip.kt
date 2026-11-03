package app.viajor.ui.presenter

import app.viajor.data.TripSummary
import kotlinx.datetime.LocalDate

enum class HeroKind { HAPPENING_NOW, NEXT }

data class HeroTrip(val summary: TripSummary, val kind: HeroKind)

/**
 * The trip the home screen highlights: one happening today, else the one that starts soonest. Undated and past
 * trips are never labelled "next", so the label is always true.
 */
fun pickHeroTrip(trips: List<TripSummary>, today: LocalDate): HeroTrip? {
    val dated = trips.filter { it.trip.startDate != null && it.trip.endDate != null }
    dated.firstOrNull { it.trip.startDate!! <= today && today <= it.trip.endDate!! }?.let { return HeroTrip(it, HeroKind.HAPPENING_NOW) }
    return dated.filter { it.trip.startDate!! > today }.minByOrNull { it.trip.startDate!! }?.let { HeroTrip(it, HeroKind.NEXT) }
}
