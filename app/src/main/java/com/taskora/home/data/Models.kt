package com.taskora.home.data

import kotlinx.serialization.Serializable

/**
 * All persistent data models for Taskora Home.
 *
 * Every model is [Serializable] (kotlinx.serialization) and stored as a JSON
 * string in DataStore. Every field has a safe default so that older stored
 * JSON — written before a field existed — still deserializes cleanly.
 *
 * Dates are stored as strings:
 *   - date:      "yyyy-MM-dd"
 *   - time:      "HH:mm"
 *   - timestamp: ISO-8601 (e.g. "2026-07-10T09:30:00")
 */

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

@Serializable
enum class HomeType { Apartment, House, Townhouse, Studio, Rental, Other }

@Serializable
enum class RoomType {
    Kitchen, Bathroom, Bedroom, LivingRoom, Hallway, Laundry,
    Garage, Basement, Attic, Outdoor, Utility, Custom
}

@Serializable
enum class MaintenanceCategory {
    Filter, Lighting, Appliance, Cleaning, InspectionReminder,
    SmallRepair, Seasonal, SafetyCheckReminder, Outdoor, Other
}

@Serializable
enum class MaintenanceScheduleType {
    OneTime, EveryNumberOfDays, EveryNumberOfWeeks, EveryNumberOfMonths,
    SelectedMonths, Yearly, ManualOnly
}

@Serializable
enum class IntervalUnit { Days, Weeks, Months, Years }

@Serializable
enum class TaskPriority { Normal, High }

@Serializable
enum class NextDateCalculationMode { FromCompletionDate, FromScheduledDate }

@Serializable
enum class ShoppingCategory {
    Filters, Bulbs, CleaningSupplies, Hardware, ApplianceSupplies,
    Outdoor, General, Other
}

@Serializable
enum class ShoppingPriority { Normal, High }

@Serializable
enum class WeekDay { Monday, Sunday }

// ---------------------------------------------------------------------------
// Value objects
// ---------------------------------------------------------------------------

@Serializable
data class MapPosition(
    val row: Int = 0,
    val column: Int = 0,
    val widthUnits: Int = 1,
    val heightUnits: Int = 1
)

// ---------------------------------------------------------------------------
// Core models
// ---------------------------------------------------------------------------

@Serializable
data class HomeProfile(
    val id: String = "",
    val name: String = "",
    val homeType: HomeType = HomeType.House,
    val description: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class Room(
    val id: String = "",
    val homeId: String = "",
    val name: String = "",
    val roomType: RoomType = RoomType.Custom,
    val colorKey: String = "utility",
    val mapPosition: MapPosition = MapPosition(),
    val sortOrder: Int = 0,
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class MaintenanceTask(
    val id: String = "",
    val homeId: String = "",
    /** null => assigned to Whole Home. */
    val roomId: String? = null,
    val title: String = "",
    val category: MaintenanceCategory = MaintenanceCategory.Other,
    val scheduleType: MaintenanceScheduleType = MaintenanceScheduleType.ManualOnly,
    val intervalValue: Int? = null,
    val intervalUnit: IntervalUnit? = null,
    val selectedMonths: List<Int> = emptyList(),
    /** Due date for OneTime tasks, "yyyy-MM-dd". */
    val specificDate: String = "",
    /** Anchor date for recurring tasks, "yyyy-MM-dd". */
    val startDate: String = "",
    val yearlyMonth: Int? = null,
    val yearlyDay: Int? = null,
    val enabled: Boolean = true,
    val priority: TaskPriority = TaskPriority.Normal,
    val calculationMode: NextDateCalculationMode = NextDateCalculationMode.FromCompletionDate,
    val notes: String = "",
    val shoppingItemLabel: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class MaintenanceCompletion(
    val id: String = "",
    val taskId: String = "",
    val homeId: String = "",
    val roomId: String? = null,
    val completedDate: String = "",
    val completedTime: String = "",
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class ShoppingItem(
    val id: String = "",
    val homeId: String = "",
    val roomId: String? = null,
    val linkedTaskId: String? = null,
    val title: String = "",
    val category: ShoppingCategory = ShoppingCategory.General,
    val quantityLabel: String = "",
    val priority: ShoppingPriority = ShoppingPriority.Normal,
    val checked: Boolean = false,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

@Serializable
data class ReminderSettings(
    val enabled: Boolean = true,
    val showOverdue: Boolean = true,
    val showDueToday: Boolean = true,
    val showShoppingReminder: Boolean = true
)

@Serializable
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val activeHomeId: String? = null,
    val soonThresholdDays: Int = 7,
    val firstDayOfWeek: WeekDay = WeekDay.Monday,
    val defaultCalculationMode: NextDateCalculationMode = NextDateCalculationMode.FromCompletionDate,
    val reminderSettings: ReminderSettings = ReminderSettings()
)

// ---------------------------------------------------------------------------
// Aggregate
// ---------------------------------------------------------------------------

@Serializable
data class AppData(
    val homes: List<HomeProfile> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val maintenanceTasks: List<MaintenanceTask> = emptyList(),
    val completions: List<MaintenanceCompletion> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val settings: AppSettings = AppSettings()
)

// ---------------------------------------------------------------------------
// Derived (non-persistent) status enum
// ---------------------------------------------------------------------------

/** Status of a single maintenance task, computed locally (never persisted). */
enum class TaskStatus { Good, Soon, Overdue, Completed, Unscheduled, Disabled, InvalidSchedule }

/** Status of a room or the whole home, aggregated from its tasks. */
enum class ZoneStatus { Good, Soon, Overdue, NoTasks }
