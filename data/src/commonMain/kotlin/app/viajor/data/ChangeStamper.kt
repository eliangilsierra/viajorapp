package app.viajor.data

import app.viajor.domain.HlcClock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** The sync metadata written with every change (ADR-0002): an HLC stamp, the device id and the wall time. */
data class Stamp(val hlc: String, val deviceId: String, val millis: Long)

/**
 * Produces stamps for one device. Call it inside the transaction that writes, so stamps follow write order.
 * [wallClockMillis] is injected so tests control time.
 */
class ChangeStamper(private val deviceId: String, private val wallClockMillis: () -> Long) {
    private val clock = HlcClock(deviceId)

    fun next(): Stamp {
        val now = wallClockMillis()
        return Stamp(clock.now(now).toString(), deviceId, now)
    }
}

/** Client-generated identifiers (ADR-0002), so records can merge across devices. */
fun interface IdGenerator {
    fun newId(): String

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        val Random = IdGenerator { Uuid.random().toString() }
    }
}
