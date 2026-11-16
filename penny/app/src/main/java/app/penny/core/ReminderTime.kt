package app.penny.core

import java.time.LocalTime
import java.time.ZonedDateTime

object ReminderTime {
    /**
     * Next occurrence of [hour]:[minute] strictly after [now], in [now]'s time zone.
     * Uses [ZonedDateTime] arithmetic so daylight-saving transitions resolve to a valid local time.
     */
    fun nextTriggerMillis(now: ZonedDateTime, hour: Int, minute: Int): Long {
        var next = now.with(LocalTime.of(hour, minute))
        if (!next.isAfter(now)) next = next.plusDays(1).with(LocalTime.of(hour, minute))
        return next.toInstant().toEpochMilli()
    }
}
