package qbit.platform

actual typealias Instant = java.time.Instant

actual object Instants {
    actual fun ofEpochMilli(epochMilli: Long): Instant = Instant.ofEpochMilli(epochMilli)
    actual fun ofEpochSecond(epochSecond: Long, nanoAdjustment: Long): Instant = Instant.ofEpochSecond(epochSecond, nanoAdjustment)
    actual fun now(): Instant = Instant.now()
}

actual typealias ZonedDateTime = java.time.ZonedDateTime

actual object ZonedDateTimes {
    actual fun of(year: Int, month: Int, dayOfMonth: Int,
                  hour: Int, minute: Int, second: Int, nanoOfSecond: Int, zone: ZoneId): ZonedDateTime =
            ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zone)
    actual fun ofInstant(instant: Instant, zone: ZoneId): ZonedDateTime = ZonedDateTime.ofInstant(instant, zone)
    actual fun now(): ZonedDateTime = ZonedDateTime.now()
    actual fun now(zone: ZoneId): ZonedDateTime = ZonedDateTime.now(zone)
    actual fun parse(text: CharSequence, formatter: DateTimeFormatter): ZonedDateTime = ZonedDateTime.parse(text, formatter)
}

actual typealias ZoneId = java.time.ZoneId
actual object ZoneIds {
    actual fun of(zoneId: String): ZoneId = ZoneId.of(zoneId)

}

actual typealias DateTimeFormatter = java.time.format.DateTimeFormatter
actual object DateTimeFormatters {
    actual fun ofPattern(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
}

actual typealias ZoneOffset = java.time.ZoneOffset
actual object ZoneOffsets {
    actual fun ofHours(hours: Int): ZoneOffset = ZoneOffset.ofHours(hours)
}

actual typealias SimpleDateFormat = java.text.SimpleDateFormat

actual typealias Date = java.util.Date