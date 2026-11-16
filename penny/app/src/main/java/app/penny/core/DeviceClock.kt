package app.penny.core

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** A [Clock] that always uses the device's *current* time zone, so "today" stays correct after travel or zone changes. */
class DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = system(zone)
    override fun instant(): Instant = Instant.now()
}
