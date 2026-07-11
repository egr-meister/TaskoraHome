package com.taskora.home.ui.navigation

/** Central route definitions and argument keys for Navigation Compose. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME_SETUP = "home_setup"

    // Bottom-nav destinations
    const val HOME_MAP = "home_map"
    const val ALL_TASKS = "all_tasks"
    const val CALENDAR = "calendar"
    const val SHOPPING = "shopping"
    const val SETTINGS = "settings"

    // Detail / editor destinations
    const val ROOM_DETAIL = "room_detail"          // room_detail/{roomId}
    const val ROOM_MANAGEMENT = "room_management"
    const val ADD_EDIT_ROOM = "add_edit_room"      // add_edit_room?roomId=
    const val ADD_EDIT_TASK = "add_edit_task"      // add_edit_task?taskId=&roomId=
    const val TASK_DETAIL = "task_detail"          // task_detail/{taskId}
    const val HISTORY = "history"
    const val COMPLETION_DETAIL = "completion_detail" // completion_detail/{completionId}
    const val HOME_PROFILES = "home_profiles"

    // Argument keys
    const val ARG_ROOM_ID = "roomId"
    const val ARG_TASK_ID = "taskId"
    const val ARG_COMPLETION_ID = "completionId"
    const val ARG_FILTER = "filter"

    fun roomDetail(roomId: String) = "$ROOM_DETAIL/$roomId"
    fun taskDetail(taskId: String) = "$TASK_DETAIL/$taskId"
    fun completionDetail(id: String) = "$COMPLETION_DETAIL/$id"

    fun addEditRoom(roomId: String? = null): String =
        if (roomId == null) ADD_EDIT_ROOM else "$ADD_EDIT_ROOM?$ARG_ROOM_ID=$roomId"

    fun addEditTask(taskId: String? = null, roomId: String? = null): String {
        val params = buildList {
            if (taskId != null) add("$ARG_TASK_ID=$taskId")
            if (roomId != null) add("$ARG_ROOM_ID=$roomId")
        }
        return if (params.isEmpty()) ADD_EDIT_TASK else "$ADD_EDIT_TASK?${params.joinToString("&")}"
    }

    fun allTasks(filter: String? = null): String =
        if (filter == null) ALL_TASKS else "$ALL_TASKS?$ARG_FILTER=$filter"
}

/** Task list filter keys used by the All Tasks screen tabs. */
object TaskFilterKeys {
    const val ALL = "all"
    const val DUE_SOON = "soon"
    const val OVERDUE = "overdue"
    const val GOOD = "good"
    const val UNSCHEDULED = "unscheduled"
    const val DISABLED = "disabled"
}
