package com.taskora.home.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Date/time helpers. All calculations use [LocalDate] / [LocalTime] — never
 * millisecond arithmetic — so month and year intervals stay calendar-correct.
 *
 * Storage formats:
 *   date      -> "yyyy-MM-dd"
 *   time      -> "HH:mm"
 *   timestamp -> ISO-8601 local date-time
 */

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DISPLAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val DISPLAY_SHORT_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val MONTH_YEAR_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

/** Generates a locally-unique id with a readable prefix. */
fun newId(prefix: String): String = "${prefix}_${UUID.randomUUID()}"

fun today(): LocalDate = LocalDate.now()

fun nowDateString(): String = LocalDate.now().format(DATE_FMT)

fun nowTimeString(): String = LocalTime.now().format(TIME_FMT)

fun nowTimestamp(): String = LocalDateTime.now().toString()

/** Parses "yyyy-MM-dd" safely; returns null on any failure. */
fun parseDate(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw.trim(), DATE_FMT)
    } catch (_: Exception) {
        // Second chance: tolerate ISO-like strings.
        try {
            LocalDate.parse(raw.trim().substring(0, minOf(10, raw.trim().length)), DATE_FMT)
        } catch (_: Exception) {
            null
        }
    }
}

fun formatDateString(date: LocalDate): String = date.format(DATE_FMT)

/** Human friendly full date, or fallback text if unparseable. */
fun displayDate(raw: String?, fallback: String = "Schedule unavailable"): String {
    val d = parseDate(raw) ?: return fallback
    return d.format(DISPLAY_FMT)
}

fun displayDate(date: LocalDate): String = date.format(DISPLAY_FMT)

fun displayDateShort(date: LocalDate): String = date.format(DISPLAY_SHORT_FMT)

fun displayMonthYear(date: LocalDate): String = date.format(MONTH_YEAR_FMT)

/** Whole days from today to [date]; negative when overdue. Null-safe. */
fun daysFromToday(date: LocalDate?): Long? {
    if (date == null) return null
    return java.time.temporal.ChronoUnit.DAYS.between(today(), date)
}

/** Relative label such as "Today", "In 3 days", "2 days ago". */
fun relativeDueLabel(date: LocalDate?): String {
    val diff = daysFromToday(date) ?: return "Schedule unavailable"
    return when {
        diff == 0L -> "Today"
        diff == 1L -> "Tomorrow"
        diff == -1L -> "Yesterday"
        diff > 1L -> "In $diff days"
        else -> "${-diff} days ago"
    }
}
