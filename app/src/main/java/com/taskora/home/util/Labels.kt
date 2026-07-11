package com.taskora.home.util

import com.taskora.home.data.HomeType
import com.taskora.home.data.IntervalUnit
import com.taskora.home.data.MaintenanceCategory
import com.taskora.home.data.MaintenanceScheduleType
import com.taskora.home.data.NextDateCalculationMode
import com.taskora.home.data.RoomType
import com.taskora.home.data.ShoppingCategory
import com.taskora.home.data.TaskStatus
import com.taskora.home.data.ZoneStatus

/** Central place for human-readable labels for enums (keeps UI text consistent). */

fun homeTypeLabel(t: HomeType): String = when (t) {
    HomeType.Apartment -> "Apartment"
    HomeType.House -> "House"
    HomeType.Townhouse -> "Townhouse"
    HomeType.Studio -> "Studio"
    HomeType.Rental -> "Rental"
    HomeType.Other -> "Other"
}

fun roomTypeLabel(t: RoomType): String = when (t) {
    RoomType.Kitchen -> "Kitchen"
    RoomType.Bathroom -> "Bathroom"
    RoomType.Bedroom -> "Bedroom"
    RoomType.LivingRoom -> "Living Room"
    RoomType.Hallway -> "Hallway"
    RoomType.Laundry -> "Laundry"
    RoomType.Garage -> "Garage"
    RoomType.Basement -> "Basement"
    RoomType.Attic -> "Attic"
    RoomType.Outdoor -> "Outdoor"
    RoomType.Utility -> "Utility"
    RoomType.Custom -> "Custom Room"
}

fun categoryLabel(c: MaintenanceCategory): String = when (c) {
    MaintenanceCategory.Filter -> "Filter"
    MaintenanceCategory.Lighting -> "Lighting"
    MaintenanceCategory.Appliance -> "Appliance"
    MaintenanceCategory.Cleaning -> "Cleaning"
    MaintenanceCategory.InspectionReminder -> "Inspection Reminder"
    MaintenanceCategory.SmallRepair -> "Small Repair"
    MaintenanceCategory.Seasonal -> "Seasonal"
    MaintenanceCategory.SafetyCheckReminder -> "Safety Check Reminder"
    MaintenanceCategory.Outdoor -> "Outdoor"
    MaintenanceCategory.Other -> "Other"
}

fun scheduleTypeLabel(s: MaintenanceScheduleType): String = when (s) {
    MaintenanceScheduleType.OneTime -> "One time"
    MaintenanceScheduleType.EveryNumberOfDays -> "Every N days"
    MaintenanceScheduleType.EveryNumberOfWeeks -> "Every N weeks"
    MaintenanceScheduleType.EveryNumberOfMonths -> "Every N months"
    MaintenanceScheduleType.SelectedMonths -> "Selected months"
    MaintenanceScheduleType.Yearly -> "Yearly"
    MaintenanceScheduleType.ManualOnly -> "Manual only"
}

fun intervalUnitLabel(u: IntervalUnit): String = when (u) {
    IntervalUnit.Days -> "Days"
    IntervalUnit.Weeks -> "Weeks"
    IntervalUnit.Months -> "Months"
    IntervalUnit.Years -> "Years"
}

fun calcModeLabel(m: NextDateCalculationMode): String = when (m) {
    NextDateCalculationMode.FromCompletionDate -> "From completion date"
    NextDateCalculationMode.FromScheduledDate -> "From scheduled date"
}

fun shoppingCategoryLabel(c: ShoppingCategory): String = when (c) {
    ShoppingCategory.Filters -> "Filters"
    ShoppingCategory.Bulbs -> "Bulbs"
    ShoppingCategory.CleaningSupplies -> "Cleaning Supplies"
    ShoppingCategory.Hardware -> "Hardware"
    ShoppingCategory.ApplianceSupplies -> "Appliance Supplies"
    ShoppingCategory.Outdoor -> "Outdoor"
    ShoppingCategory.General -> "General"
    ShoppingCategory.Other -> "Other"
}

fun taskStatusLabel(s: TaskStatus): String = when (s) {
    TaskStatus.Good -> "Good"
    TaskStatus.Soon -> "Soon"
    TaskStatus.Overdue -> "Overdue"
    TaskStatus.Completed -> "Completed"
    TaskStatus.Unscheduled -> "Unscheduled"
    TaskStatus.Disabled -> "Disabled"
    TaskStatus.InvalidSchedule -> "Schedule unavailable"
}

fun zoneStatusLabel(s: ZoneStatus): String = when (s) {
    ZoneStatus.Good -> "Good"
    ZoneStatus.Soon -> "Soon"
    ZoneStatus.Overdue -> "Overdue"
    ZoneStatus.NoTasks -> "No Tasks"
}

/** Month number (1-12) to short name; safe for out-of-range values. */
fun monthShort(month: Int): String {
    val names = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    return names.getOrElse(month - 1) { "?" }
}

/** Builds a compact recurrence description for a task. */
fun recurrenceLabel(
    scheduleType: MaintenanceScheduleType,
    intervalValue: Int?,
    selectedMonths: List<Int>,
    yearlyMonth: Int?,
    yearlyDay: Int?
): String = when (scheduleType) {
    MaintenanceScheduleType.OneTime -> "One time"
    MaintenanceScheduleType.ManualOnly -> "Manual only"
    MaintenanceScheduleType.EveryNumberOfDays -> "Every ${intervalValue ?: 0} day(s)"
    MaintenanceScheduleType.EveryNumberOfWeeks -> "Every ${intervalValue ?: 0} week(s)"
    MaintenanceScheduleType.EveryNumberOfMonths -> "Every ${intervalValue ?: 0} month(s)"
    MaintenanceScheduleType.SelectedMonths -> {
        val names = selectedMonths.filter { it in 1..12 }.sorted().joinToString(", ") { monthShort(it) }
        if (names.isBlank()) "Selected months" else "Months: $names"
    }
    MaintenanceScheduleType.Yearly -> {
        if (yearlyMonth != null && yearlyDay != null) "Yearly on ${monthShort(yearlyMonth)} $yearlyDay"
        else "Yearly"
    }
}
