package com.taskora.home.util

import com.taskora.home.data.IntervalUnit
import com.taskora.home.data.MaintenanceScheduleType

/** Reasonable maximums for interval values, keyed by unit. */
object IntervalLimits {
    const val MAX_DAYS = 3650
    const val MAX_WEEKS = 520
    const val MAX_MONTHS = 120
    const val MAX_YEARS = 50

    fun max(unit: IntervalUnit): Int = when (unit) {
        IntervalUnit.Days -> MAX_DAYS
        IntervalUnit.Weeks -> MAX_WEEKS
        IntervalUnit.Months -> MAX_MONTHS
        IntervalUnit.Years -> MAX_YEARS
    }

    fun maxForSchedule(type: MaintenanceScheduleType): Int = when (type) {
        MaintenanceScheduleType.EveryNumberOfDays -> MAX_DAYS
        MaintenanceScheduleType.EveryNumberOfWeeks -> MAX_WEEKS
        MaintenanceScheduleType.EveryNumberOfMonths -> MAX_MONTHS
        else -> MAX_YEARS
    }
}

/** Note length limits. */
object NoteLimits {
    const val SHORT = 300
    const val DETAILED = 1000
}

/** Result of validating an input form. */
data class ValidationResult(
    val errors: Map<String, String> = emptyMap()
) {
    val isValid: Boolean get() = errors.isEmpty()
    fun error(field: String): String? = errors[field]

    companion object {
        const val FIELD_TITLE = "title"
        const val FIELD_NAME = "name"
        const val FIELD_INTERVAL = "interval"
        const val FIELD_START = "startDate"
        const val FIELD_DUE = "dueDate"
        const val FIELD_MONTHS = "months"
        const val FIELD_YEARLY = "yearly"
    }
}

object Validation {

    /** Parses an interval text field. Empty is allowed while typing (returns null). */
    fun parseIntervalInput(raw: String): Int? = raw.trim().toIntOrNull()

    /** Clamps and sanitizes an interval to its unit maximum. */
    fun clampInterval(value: Int, unit: IntervalUnit): Int =
        value.coerceIn(1, IntervalLimits.max(unit))

    fun validateHome(name: String): ValidationResult {
        val errors = mutableMapOf<String, String>()
        if (name.trim().isBlank()) errors[ValidationResult.FIELD_NAME] = "Home name is required."
        return ValidationResult(errors)
    }

    fun validateRoom(name: String): ValidationResult {
        val errors = mutableMapOf<String, String>()
        if (name.trim().isBlank()) errors[ValidationResult.FIELD_NAME] = "Room name is required."
        return ValidationResult(errors)
    }

    /**
     * Validates a maintenance task form. [intervalText] is the raw text field.
     */
    fun validateTask(
        title: String,
        scheduleType: MaintenanceScheduleType,
        intervalText: String,
        intervalUnit: IntervalUnit,
        startDate: String,
        specificDate: String,
        selectedMonths: List<Int>,
        yearlyMonth: Int?,
        yearlyDay: Int?
    ): ValidationResult {
        val errors = mutableMapOf<String, String>()

        if (title.trim().isBlank()) {
            errors[ValidationResult.FIELD_TITLE] = "Task title is required."
        }

        when (scheduleType) {
            MaintenanceScheduleType.ManualOnly -> { /* no schedule fields required */ }

            MaintenanceScheduleType.OneTime -> {
                if (parseDate(specificDate) == null) {
                    errors[ValidationResult.FIELD_DUE] = "A valid due date is required."
                }
            }

            MaintenanceScheduleType.EveryNumberOfDays,
            MaintenanceScheduleType.EveryNumberOfWeeks,
            MaintenanceScheduleType.EveryNumberOfMonths -> {
                val n = parseIntervalInput(intervalText)
                if (n == null || n <= 0) {
                    errors[ValidationResult.FIELD_INTERVAL] = "Interval must be greater than zero."
                } else if (n > IntervalLimits.maxForSchedule(scheduleType)) {
                    errors[ValidationResult.FIELD_INTERVAL] =
                        "Interval is too large (max ${IntervalLimits.maxForSchedule(scheduleType)})."
                }
                if (parseDate(startDate) == null) {
                    errors[ValidationResult.FIELD_START] = "A valid start date is required."
                }
            }

            MaintenanceScheduleType.SelectedMonths -> {
                if (selectedMonths.none { it in 1..12 }) {
                    errors[ValidationResult.FIELD_MONTHS] = "Select at least one month."
                }
                if (parseDate(startDate) == null) {
                    errors[ValidationResult.FIELD_START] = "A valid start date is required."
                }
            }

            MaintenanceScheduleType.Yearly -> {
                if (yearlyMonth == null || yearlyMonth !in 1..12 ||
                    yearlyDay == null || yearlyDay !in 1..31
                ) {
                    errors[ValidationResult.FIELD_YEARLY] = "Choose a valid month and day."
                }
                if (parseDate(startDate) == null) {
                    errors[ValidationResult.FIELD_START] = "A valid start date is required."
                }
            }
        }

        return ValidationResult(errors)
    }

    fun trimNote(raw: String, limit: Int): String =
        raw.trim().let { if (it.length > limit) it.substring(0, limit) else it }
}
