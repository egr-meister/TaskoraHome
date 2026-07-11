package com.taskora.home.ui.screens

import com.taskora.home.data.AppData
import com.taskora.home.ui.viewmodel.TaskoraViewModel

/** Resolves a display name for a task's room, with safe fallbacks. */
fun roomName(vm: TaskoraViewModel, data: AppData, roomId: String?): String {
    if (roomId == null) return "Whole Home"
    return vm.roomById(roomId, data)?.name?.ifBlank { "Room" } ?: "Deleted Room"
}
