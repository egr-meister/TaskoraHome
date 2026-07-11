package com.taskora.home.util

import com.taskora.home.data.MaintenanceCategory
import com.taskora.home.data.MapPosition
import com.taskora.home.data.RoomType

/**
 * Local, editable organizational examples. These create only room placeholders
 * or task labels — never repair steps or professional advice. The user reviews
 * and customizes everything.
 */

data class RoomSpec(
    val name: String,
    val type: RoomType,
    val colorKey: String,
    val mapPosition: MapPosition
)

data class LayoutTemplate(
    val key: String,
    val name: String,
    val description: String,
    val rooms: List<RoomSpec>
)

data class TaskTemplate(
    val title: String,
    val category: MaintenanceCategory
)

data class TaskTemplateGroup(
    val name: String,
    val note: String,
    val tasks: List<TaskTemplate>
)

object Templates {

    val NONE_KEY = "custom_empty"

    val layouts: List<LayoutTemplate> = listOf(
        LayoutTemplate(
            key = "compact_apartment",
            name = "Compact Apartment",
            description = "A small set of rooms for an apartment or studio.",
            rooms = listOf(
                RoomSpec("Kitchen", RoomType.Kitchen, "kitchen", MapPosition(0, 0)),
                RoomSpec("Bathroom", RoomType.Bathroom, "bathroom", MapPosition(0, 1)),
                RoomSpec("Living Room", RoomType.LivingRoom, "living", MapPosition(1, 0)),
                RoomSpec("Bedroom", RoomType.Bedroom, "bedroom", MapPosition(1, 1))
            )
        ),
        LayoutTemplate(
            key = "standard_home",
            name = "Standard Home",
            description = "A common layout for a house or townhouse.",
            rooms = listOf(
                RoomSpec("Kitchen", RoomType.Kitchen, "kitchen", MapPosition(0, 0)),
                RoomSpec("Living Room", RoomType.LivingRoom, "living", MapPosition(0, 1)),
                RoomSpec("Bathroom", RoomType.Bathroom, "bathroom", MapPosition(1, 0)),
                RoomSpec("Bedroom", RoomType.Bedroom, "bedroom", MapPosition(1, 1)),
                RoomSpec("Hallway", RoomType.Hallway, "utility", MapPosition(2, 0)),
                RoomSpec("Laundry", RoomType.Laundry, "laundry", MapPosition(2, 1))
            )
        ),
        LayoutTemplate(
            key = "utility_focus",
            name = "Utility Focus",
            description = "Emphasizes utility and service areas.",
            rooms = listOf(
                RoomSpec("Kitchen", RoomType.Kitchen, "kitchen", MapPosition(0, 0)),
                RoomSpec("Laundry", RoomType.Laundry, "laundry", MapPosition(0, 1)),
                RoomSpec("Garage", RoomType.Garage, "garage", MapPosition(1, 0)),
                RoomSpec("Utility", RoomType.Utility, "utility", MapPosition(1, 1)),
                RoomSpec("Basement", RoomType.Basement, "garage", MapPosition(2, 0)),
                RoomSpec("Outdoor", RoomType.Outdoor, "outdoor", MapPosition(2, 1))
            )
        ),
        LayoutTemplate(
            key = NONE_KEY,
            name = "Custom Rooms",
            description = "Start with no rooms and add your own.",
            rooms = emptyList()
        )
    )

    fun layoutByKey(key: String?): LayoutTemplate? = layouts.firstOrNull { it.key == key }

    val taskGroups: List<TaskTemplateGroup> = listOf(
        TaskTemplateGroup(
            name = "Kitchen Routine",
            note = "Editable organizational examples, not professional advice.",
            tasks = listOf(
                TaskTemplate("Review water filter date", MaintenanceCategory.Filter),
                TaskTemplate("Clean appliance exterior", MaintenanceCategory.Cleaning),
                TaskTemplate("Check light bulbs", MaintenanceCategory.Lighting),
                TaskTemplate("Review refrigerator seal visually", MaintenanceCategory.InspectionReminder)
            )
        ),
        TaskTemplateGroup(
            name = "Bathroom Routine",
            note = "Editable organizational examples, not professional advice.",
            tasks = listOf(
                TaskTemplate("Clean ventilation cover", MaintenanceCategory.Cleaning),
                TaskTemplate("Review light bulbs", MaintenanceCategory.Lighting),
                TaskTemplate("Check supply list", MaintenanceCategory.Other),
                TaskTemplate("Clean accessible surfaces", MaintenanceCategory.Cleaning)
            )
        ),
        TaskTemplateGroup(
            name = "Laundry Routine",
            note = "Editable organizational examples, not professional advice.",
            tasks = listOf(
                TaskTemplate("Clean lint filter reminder", MaintenanceCategory.Filter),
                TaskTemplate("Review hoses visually", MaintenanceCategory.InspectionReminder),
                TaskTemplate("Clean detergent drawer", MaintenanceCategory.Cleaning),
                TaskTemplate("Check supply inventory", MaintenanceCategory.Other)
            )
        ),
        TaskTemplateGroup(
            name = "Seasonal Routine",
            note = "Editable organizational examples, not professional advice.",
            tasks = listOf(
                TaskTemplate("Review outdoor lighting", MaintenanceCategory.Outdoor),
                TaskTemplate("Review weather seal condition visually", MaintenanceCategory.Seasonal),
                TaskTemplate("Check seasonal supply list", MaintenanceCategory.Seasonal),
                TaskTemplate("Review filter replacement dates", MaintenanceCategory.Filter)
            )
        )
    )
}
