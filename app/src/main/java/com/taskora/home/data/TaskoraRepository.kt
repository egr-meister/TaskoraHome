package com.taskora.home.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taskora.home.util.newId
import com.taskora.home.util.nowTimeString
import com.taskora.home.util.nowTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/** DataStore instance scoped to the application context. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taskora_home")

/**
 * Single repository for all Taskora Home data.
 *
 * Responsibilities:
 *  - Own the DataStore instance and JSON (de)serialization.
 *  - Expose the whole [AppData] as a [Flow].
 *  - Provide guarded CRUD operations for homes, rooms, tasks, completions,
 *    shopping items and settings.
 *  - Deserialize defensively: missing keys, empty strings, corrupted JSON and
 *    newly-added fields never crash the app.
 *
 * Item-level recovery: each collection is parsed independently, so a single
 * corrupted collection falls back to empty without discarding the others.
 */
class TaskoraRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true      // tolerate removed fields
        encodeDefaults = true         // always write defaults for forward-compat
        isLenient = true
        coerceInputValues = true      // null -> default for non-null fields
    }

    private object Keys {
        val HOMES = stringPreferencesKey("homes_json")
        val ROOMS = stringPreferencesKey("rooms_json")
        val TASKS = stringPreferencesKey("maintenance_tasks_json")
        val COMPLETIONS = stringPreferencesKey("maintenance_completions_json")
        val SHOPPING = stringPreferencesKey("shopping_items_json")
        val SETTINGS = stringPreferencesKey("settings_json")
    }

    // ---- Safe decoders -----------------------------------------------------

    private inline fun <reified T> decodeList(raw: String?): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<T>>(raw)
        } catch (_: Exception) {
            // Corrupted collection — recover to empty rather than crash.
            emptyList()
        }
    }

    private fun decodeSettings(raw: String?): AppSettings {
        if (raw.isNullOrBlank()) return AppSettings()
        return try {
            json.decodeFromString<AppSettings>(raw)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    // ---- Observable state --------------------------------------------------

    val appData: Flow<AppData> = context.dataStore.data.map { prefs ->
        AppData(
            homes = decodeList(prefs[Keys.HOMES]),
            rooms = decodeList(prefs[Keys.ROOMS]),
            maintenanceTasks = decodeList(prefs[Keys.TASKS]),
            completions = decodeList(prefs[Keys.COMPLETIONS]),
            shoppingItems = decodeList(prefs[Keys.SHOPPING]),
            settings = decodeSettings(prefs[Keys.SETTINGS])
        )
    }

    /** Reads the current snapshot once (used inside mutating operations). */
    private suspend fun current(): AppData {
        val prefs = context.dataStore.data.first()
        return AppData(
            homes = decodeList(prefs[Keys.HOMES]),
            rooms = decodeList(prefs[Keys.ROOMS]),
            maintenanceTasks = decodeList(prefs[Keys.TASKS]),
            completions = decodeList(prefs[Keys.COMPLETIONS]),
            shoppingItems = decodeList(prefs[Keys.SHOPPING]),
            settings = decodeSettings(prefs[Keys.SETTINGS])
        )
    }

    private suspend fun writeAll(data: AppData) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOMES] = json.encodeToString(data.homes)
            prefs[Keys.ROOMS] = json.encodeToString(data.rooms)
            prefs[Keys.TASKS] = json.encodeToString(data.maintenanceTasks)
            prefs[Keys.COMPLETIONS] = json.encodeToString(data.completions)
            prefs[Keys.SHOPPING] = json.encodeToString(data.shoppingItems)
            prefs[Keys.SETTINGS] = json.encodeToString(data.settings)
        }
    }

    // =======================================================================
    // Homes
    // =======================================================================

    suspend fun addHome(name: String, homeType: HomeType, description: String): HomeProfile {
        val data = current()
        val ts = nowTimestamp()
        val home = HomeProfile(
            id = newId("home"),
            name = name.trim(),
            homeType = homeType,
            description = description.trim(),
            createdAt = ts,
            updatedAt = ts
        )
        val settings = if (data.settings.activeHomeId == null) {
            data.settings.copy(activeHomeId = home.id)
        } else data.settings
        writeAll(data.copy(homes = data.homes + home, settings = settings))
        return home
    }

    suspend fun updateHome(home: HomeProfile) {
        val data = current()
        val updated = data.homes.map {
            if (it.id == home.id) home.copy(updatedAt = nowTimestamp()) else it
        }
        writeAll(data.copy(homes = updated))
    }

    suspend fun setActiveHome(homeId: String) {
        val data = current()
        if (data.homes.none { it.id == homeId }) return
        writeAll(data.copy(settings = data.settings.copy(activeHomeId = homeId)))
    }

    /** Deletes a home and every dependent record (rooms, tasks, completions, shopping). */
    suspend fun deleteHome(homeId: String) {
        val data = current()
        val remainingHomes = data.homes.filterNot { it.id == homeId }
        val newActive = when {
            data.settings.activeHomeId != homeId -> data.settings.activeHomeId
            remainingHomes.isNotEmpty() -> remainingHomes.first().id
            else -> null
        }
        writeAll(
            data.copy(
                homes = remainingHomes,
                rooms = data.rooms.filterNot { it.homeId == homeId },
                maintenanceTasks = data.maintenanceTasks.filterNot { it.homeId == homeId },
                completions = data.completions.filterNot { it.homeId == homeId },
                shoppingItems = data.shoppingItems.filterNot { it.homeId == homeId },
                settings = data.settings.copy(activeHomeId = newActive)
            )
        )
    }

    // =======================================================================
    // Rooms
    // =======================================================================

    suspend fun addRoom(room: Room): Room {
        val data = current()
        val ts = nowTimestamp()
        val maxOrder = data.rooms.filter { it.homeId == room.homeId }.maxOfOrNull { it.sortOrder } ?: -1
        val toStore = room.copy(
            id = room.id.ifBlank { newId("room") },
            sortOrder = if (room.sortOrder == 0) maxOrder + 1 else room.sortOrder,
            createdAt = ts,
            updatedAt = ts
        )
        writeAll(data.copy(rooms = data.rooms + toStore))
        return toStore
    }

    suspend fun updateRoom(room: Room) {
        val data = current()
        val updated = data.rooms.map {
            if (it.id == room.id) room.copy(updatedAt = nowTimestamp()) else it
        }
        writeAll(data.copy(rooms = updated))
    }

    suspend fun deleteRoom(roomId: String) {
        val data = current()
        writeAll(
            data.copy(
                rooms = data.rooms.filterNot { it.id == roomId },
                maintenanceTasks = data.maintenanceTasks.filterNot { it.roomId == roomId },
                completions = data.completions.filterNot { it.roomId == roomId },
                // Shopping items keep existing but lose the room link.
                shoppingItems = data.shoppingItems.map {
                    if (it.roomId == roomId) it.copy(roomId = null) else it
                }
            )
        )
    }

    suspend fun moveRoom(homeId: String, roomId: String, up: Boolean) {
        val data = current()
        val ordered = data.rooms.filter { it.homeId == homeId }.sortedBy { it.sortOrder }
        val index = ordered.indexOfFirst { it.id == roomId }
        if (index < 0) return
        val swapWith = if (up) index - 1 else index + 1
        if (swapWith < 0 || swapWith >= ordered.size) return
        val a = ordered[index]
        val b = ordered[swapWith]
        val updated = data.rooms.map {
            when (it.id) {
                a.id -> it.copy(sortOrder = b.sortOrder, updatedAt = nowTimestamp())
                b.id -> it.copy(sortOrder = a.sortOrder, updatedAt = nowTimestamp())
                else -> it
            }
        }
        writeAll(data.copy(rooms = updated))
    }

    // =======================================================================
    // Tasks
    // =======================================================================

    suspend fun addTask(task: MaintenanceTask): MaintenanceTask {
        val data = current()
        val ts = nowTimestamp()
        val toStore = task.copy(
            id = task.id.ifBlank { newId("task") },
            createdAt = ts,
            updatedAt = ts
        )
        writeAll(data.copy(maintenanceTasks = data.maintenanceTasks + toStore))
        return toStore
    }

    suspend fun updateTask(task: MaintenanceTask) {
        val data = current()
        val updated = data.maintenanceTasks.map {
            if (it.id == task.id) task.copy(updatedAt = nowTimestamp()) else it
        }
        writeAll(data.copy(maintenanceTasks = updated))
    }

    suspend fun setTaskEnabled(taskId: String, enabled: Boolean) {
        val data = current()
        val updated = data.maintenanceTasks.map {
            if (it.id == taskId) it.copy(enabled = enabled, updatedAt = nowTimestamp()) else it
        }
        writeAll(data.copy(maintenanceTasks = updated))
    }

    suspend fun deleteTask(taskId: String) {
        val data = current()
        writeAll(
            data.copy(
                maintenanceTasks = data.maintenanceTasks.filterNot { it.id == taskId },
                completions = data.completions.filterNot { it.taskId == taskId },
                shoppingItems = data.shoppingItems.map {
                    if (it.linkedTaskId == taskId) it.copy(linkedTaskId = null) else it
                }
            )
        )
    }

    // =======================================================================
    // Completions
    // =======================================================================

    suspend fun recordCompletion(
        task: MaintenanceTask,
        completedDate: String,
        note: String
    ): MaintenanceCompletion {
        val data = current()
        val ts = nowTimestamp()
        val completion = MaintenanceCompletion(
            id = newId("done"),
            taskId = task.id,
            homeId = task.homeId,
            roomId = task.roomId,
            completedDate = completedDate,
            completedTime = nowTimeString(),
            note = note.trim(),
            createdAt = ts,
            updatedAt = ts
        )
        writeAll(data.copy(completions = data.completions + completion))
        return completion
    }

    suspend fun updateCompletionNote(completionId: String, note: String) {
        val data = current()
        val updated = data.completions.map {
            if (it.id == completionId) it.copy(note = note.trim(), updatedAt = nowTimestamp()) else it
        }
        writeAll(data.copy(completions = updated))
    }

    /**
     * Deletes a completion. The task's schedule is naturally recalculated by the
     * status utilities from the remaining completions (the previous record, if
     * any, or the start date). No stored due date is cached, so removal is safe.
     */
    suspend fun deleteCompletion(completionId: String) {
        val data = current()
        writeAll(data.copy(completions = data.completions.filterNot { it.id == completionId }))
    }

    // =======================================================================
    // Shopping
    // =======================================================================

    suspend fun addShoppingItem(item: ShoppingItem): ShoppingItem {
        val data = current()
        val ts = nowTimestamp()
        val toStore = item.copy(
            id = item.id.ifBlank { newId("shop") },
            createdAt = ts,
            updatedAt = ts
        )
        writeAll(data.copy(shoppingItems = data.shoppingItems + toStore))
        return toStore
    }

    suspend fun updateShoppingItem(item: ShoppingItem) {
        val data = current()
        val updated = data.shoppingItems.map {
            if (it.id == item.id) item.copy(updatedAt = nowTimestamp()) else it
        }
        writeAll(data.copy(shoppingItems = updated))
    }

    suspend fun setShoppingChecked(itemId: String, checked: Boolean) {
        val data = current()
        val updated = data.shoppingItems.map {
            if (it.id == itemId) it.copy(checked = checked, updatedAt = nowTimestamp()) else it
        }
        writeAll(data.copy(shoppingItems = updated))
    }

    suspend fun deleteShoppingItem(itemId: String) {
        val data = current()
        writeAll(data.copy(shoppingItems = data.shoppingItems.filterNot { it.id == itemId }))
    }

    suspend fun clearCheckedShopping(homeId: String) {
        val data = current()
        writeAll(
            data.copy(
                shoppingItems = data.shoppingItems.filterNot { it.homeId == homeId && it.checked }
            )
        )
    }

    // =======================================================================
    // Settings & resets
    // =======================================================================

    suspend fun updateSettings(settings: AppSettings) {
        val data = current()
        writeAll(data.copy(settings = settings))
    }

    suspend fun completeOnboarding() {
        val data = current()
        writeAll(data.copy(settings = data.settings.copy(onboardingCompleted = true)))
    }

    suspend fun showOnboardingAgain() {
        val data = current()
        writeAll(data.copy(settings = data.settings.copy(onboardingCompleted = false)))
    }

    suspend fun resetHistoryForHome(homeId: String) {
        val data = current()
        writeAll(data.copy(completions = data.completions.filterNot { it.homeId == homeId }))
    }

    suspend fun resetAllData() {
        context.dataStore.edit { it.clear() }
    }

    // =======================================================================
    // Derived helpers
    // =======================================================================

    /**
     * Convenience: create a quick shopping item derived from a task's
     * shopping label (used when the user taps "Add to shopping" on a task).
     */
    suspend fun createShoppingFromTask(task: MaintenanceTask): ShoppingItem? {
        val label = task.shoppingItemLabel.trim()
        if (label.isBlank()) return null
        return addShoppingItem(
            ShoppingItem(
                homeId = task.homeId,
                roomId = task.roomId,
                linkedTaskId = task.id,
                title = label,
                category = ShoppingCategory.General,
                priority = if (task.priority == TaskPriority.High) ShoppingPriority.High else ShoppingPriority.Normal
            )
        )
    }
}
