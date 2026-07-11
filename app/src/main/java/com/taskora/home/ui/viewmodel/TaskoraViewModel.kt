package com.taskora.home.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.taskora.home.data.AppData
import com.taskora.home.data.AppSettings
import com.taskora.home.data.HomeProfile
import com.taskora.home.data.HomeType
import com.taskora.home.data.MaintenanceCompletion
import com.taskora.home.data.MaintenanceTask
import com.taskora.home.data.Room
import com.taskora.home.data.ShoppingItem
import com.taskora.home.data.TaskoraRepository
import com.taskora.home.util.RoomSpec
import com.taskora.home.util.StatusCalculator
import com.taskora.home.util.TaskComputed
import com.taskora.home.util.newId
import com.taskora.home.util.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Single shared ViewModel for the whole app (simple MVVM, one repository).
 * Screens read [appData] and call action methods; all mutations run in
 * [viewModelScope] and persist through the repository.
 */
class TaskoraViewModel(private val repo: TaskoraRepository) : ViewModel() {

    val appData: StateFlow<AppData> = repo.appData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppData()
    )

    /** Becomes true once the first snapshot has been read from storage. */
    private val _bootstrapped = MutableStateFlow(false)
    val bootstrapped: StateFlow<Boolean> = _bootstrapped.asStateFlow()

    init {
        viewModelScope.launch {
            // One-shot: flip to true after the first emission from DataStore.
            repo.appData.first()
            _bootstrapped.value = true
        }
    }

    /** Key of the currently dismissed reminder, so it is not shown again this session. */
    private val _dismissedReminderKey = MutableStateFlow<String?>(null)
    val dismissedReminderKey: StateFlow<String?> = _dismissedReminderKey.asStateFlow()

    fun dismissReminder(key: String) {
        _dismissedReminderKey.value = key
    }

    fun resetReminderDismissal() {
        _dismissedReminderKey.value = null
    }

    // ---- derived read helpers (pure) --------------------------------------

    fun activeHome(data: AppData = appData.value): HomeProfile? {
        val id = data.settings.activeHomeId
        return data.homes.firstOrNull { it.id == id } ?: data.homes.firstOrNull()
    }

    fun roomsForHome(homeId: String, data: AppData = appData.value): List<Room> =
        data.rooms.filter { it.homeId == homeId }.sortedBy { it.sortOrder }

    fun computedForHome(
        homeId: String,
        data: AppData = appData.value,
        todayDate: LocalDate = today()
    ): List<TaskComputed> = StatusCalculator.computeForHome(
        homeId = homeId,
        tasks = data.maintenanceTasks,
        completions = data.completions,
        soonThresholdDays = data.settings.soonThresholdDays,
        todayDate = todayDate
    )

    fun taskById(id: String?, data: AppData = appData.value): MaintenanceTask? =
        if (id == null) null else data.maintenanceTasks.firstOrNull { it.id == id }

    fun roomById(id: String?, data: AppData = appData.value): Room? =
        if (id == null) null else data.rooms.firstOrNull { it.id == id }

    fun completionById(id: String?, data: AppData = appData.value): MaintenanceCompletion? =
        if (id == null) null else data.completions.firstOrNull { it.id == id }

    fun shoppingById(id: String?, data: AppData = appData.value): ShoppingItem? =
        if (id == null) null else data.shoppingItems.firstOrNull { it.id == id }

    // ---- home actions -----------------------------------------------------

    fun addHome(name: String, type: HomeType, description: String, onDone: (HomeProfile) -> Unit = {}) =
        viewModelScope.launch {
            val home = repo.addHome(name, type, description)
            onDone(home)
        }

    fun addHomeWithLayout(
        name: String,
        type: HomeType,
        description: String,
        rooms: List<RoomSpec>,
        onDone: (HomeProfile) -> Unit = {}
    ) = viewModelScope.launch {
        val home = repo.addHome(name, type, description)
        rooms.forEachIndexed { index, spec ->
            repo.addRoom(
                Room(
                    id = newId("room"),
                    homeId = home.id,
                    name = spec.name,
                    roomType = spec.type,
                    colorKey = spec.colorKey,
                    mapPosition = spec.mapPosition,
                    sortOrder = index
                )
            )
        }
        onDone(home)
    }

    fun updateHome(home: HomeProfile) = viewModelScope.launch { repo.updateHome(home) }
    fun setActiveHome(homeId: String) = viewModelScope.launch {
        repo.setActiveHome(homeId)
        resetReminderDismissal()
    }
    fun deleteHome(homeId: String) = viewModelScope.launch { repo.deleteHome(homeId) }

    // ---- room actions -----------------------------------------------------

    fun addRoom(room: Room, onDone: (Room) -> Unit = {}) = viewModelScope.launch {
        val saved = repo.addRoom(room)
        onDone(saved)
    }
    fun updateRoom(room: Room) = viewModelScope.launch { repo.updateRoom(room) }
    fun deleteRoom(roomId: String) = viewModelScope.launch { repo.deleteRoom(roomId) }
    fun moveRoom(homeId: String, roomId: String, up: Boolean) =
        viewModelScope.launch { repo.moveRoom(homeId, roomId, up) }

    // ---- task actions -----------------------------------------------------

    fun addTask(task: MaintenanceTask, onDone: (MaintenanceTask) -> Unit = {}) =
        viewModelScope.launch {
            val saved = repo.addTask(task)
            onDone(saved)
        }
    fun updateTask(task: MaintenanceTask) = viewModelScope.launch { repo.updateTask(task) }
    fun setTaskEnabled(taskId: String, enabled: Boolean) =
        viewModelScope.launch { repo.setTaskEnabled(taskId, enabled) }
    fun deleteTask(taskId: String) = viewModelScope.launch { repo.deleteTask(taskId) }

    // ---- completion actions ----------------------------------------------

    fun recordCompletion(task: MaintenanceTask, completedDate: String, note: String) =
        viewModelScope.launch {
            repo.recordCompletion(task, completedDate, note)
            resetReminderDismissal()
        }
    fun updateCompletionNote(completionId: String, note: String) =
        viewModelScope.launch { repo.updateCompletionNote(completionId, note) }
    fun deleteCompletion(completionId: String) =
        viewModelScope.launch { repo.deleteCompletion(completionId) }

    // ---- shopping actions -------------------------------------------------

    fun addShoppingItem(item: ShoppingItem, onDone: (ShoppingItem) -> Unit = {}) =
        viewModelScope.launch {
            val saved = repo.addShoppingItem(item)
            onDone(saved)
        }
    fun updateShoppingItem(item: ShoppingItem) =
        viewModelScope.launch { repo.updateShoppingItem(item) }
    fun setShoppingChecked(itemId: String, checked: Boolean) =
        viewModelScope.launch { repo.setShoppingChecked(itemId, checked) }
    fun deleteShoppingItem(itemId: String) =
        viewModelScope.launch { repo.deleteShoppingItem(itemId) }
    fun clearCheckedShopping(homeId: String) =
        viewModelScope.launch { repo.clearCheckedShopping(homeId) }

    // ---- settings & resets ------------------------------------------------

    fun updateSettings(settings: AppSettings) = viewModelScope.launch { repo.updateSettings(settings) }
    fun completeOnboarding() = viewModelScope.launch { repo.completeOnboarding() }
    fun showOnboardingAgain() = viewModelScope.launch { repo.showOnboardingAgain() }
    fun resetHistoryForHome(homeId: String) = viewModelScope.launch { repo.resetHistoryForHome(homeId) }
    fun resetAllData() = viewModelScope.launch {
        repo.resetAllData()
        resetReminderDismissal()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return TaskoraViewModel(TaskoraRepository(context.applicationContext)) as T
                }
            }
    }
}
