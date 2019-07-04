package qbit.platform

expect class Instant {
    fun toEpochMilli(): Long
}

expect object Instants {
    fun ofEpochMilli(epochMilli: Long): Instant
    fun ofEpochSecond(epochSecond: Long, nanoAdjustment: Long): Instant
    fun now(): Instant
}

expect class ZonedDateTime {
    fun withZoneSameInstant(zone: ZoneId): ZonedDateTime
}

expect object ZonedDateTimes {
    fun of(year: Int, month: Int, dayOfMonth: Int,
           hour: Int, minute: Int, second: Int, nanoOfSecond: Int, zone: ZoneId): ZonedDateTime
    fun ofInstant(instant: Instant, zone: ZoneId): ZonedDateTime
    fun now(): ZonedDateTime
    fun now(zone: ZoneId): ZonedDateTime
    fun parse(text: CharSequence, formatter: DateTimeFormatter): ZonedDateTime
}

expect abstract class ZoneId

expect object ZoneIds {
    fun of(zoneId: String): ZoneId
}

expect class DateTimeFormatter

expect object DateTimeFormatters {
    fun ofPattern(pattern: String): DateTimeFormatter
}

expect class ZoneOffset

expect object ZoneOffsets {
    fun ofHours(hours: Int): ZoneOffset
}