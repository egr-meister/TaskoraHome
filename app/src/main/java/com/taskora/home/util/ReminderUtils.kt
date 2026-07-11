package com.taskora.home.util

import com.taskora.home.data.AppSettings
import com.taskora.home.data.ShoppingItem
import com.taskora.home.data.ShoppingPriority
import com.taskora.home.data.TaskStatus

/** A single in-app reminder banner. Purely local; never a system notification. */
data class ReminderInfo(
    val title: String,
    val detail: String
)

object ReminderEvaluator {

    /**
     * Produces an in-app reminder from the current computed tasks and shopping
     * list, respecting user reminder settings. Returns null when nothing needs
     * attention or reminders are disabled.
     */
    fun evaluate(
        computed: List<TaskComputed>,
        shoppingItems: List<ShoppingItem>,
        settings: AppSettings
    ): ReminderInfo? {
        val r = settings.reminderSettings
        if (!r.enabled) return null

        val overdue = if (r.showOverdue) {
            computed.filter { it.task.enabled && it.status == TaskStatus.Overdue }
        } else emptyList()

        val dueToday = if (r.showDueToday) {
            computed.filter {
                it.task.enabled && it.status == TaskStatus.Soon && it.nextDue == today()
            }
        } else emptyList()

        val highShopping = if (r.showShoppingReminder) {
            shoppingItems.filter { !it.checked && it.priority == ShoppingPriority.High }
        } else emptyList()

        if (overdue.isEmpty() && dueToday.isEmpty() && highShopping.isEmpty()) return null

        val title = when {
            overdue.isNotEmpty() -> {
                val n = overdue.size
                if (n == 1) "1 maintenance task is overdue."
                else "$n maintenance tasks are overdue."
            }
            dueToday.isNotEmpty() -> {
                val n = dueToday.size
                if (n == 1) "1 maintenance task is due today."
                else "$n maintenance tasks are due today."
            }
            else -> {
                val n = highShopping.size
                if (n == 1) "1 high-priority shopping item remains."
                else "$n high-priority shopping items remain."
            }
        }

        val highlighted = (overdue + dueToday).map { it.task.title }.filter { it.isNotBlank() }.take(2)
        val detail = when {
            highlighted.isNotEmpty() -> {
                val joined = when (highlighted.size) {
                    1 -> highlighted[0]
                    else -> "${highlighted[0]} and ${highlighted[1]}"
                }
                "$joined need review."
            }
            highShopping.isNotEmpty() -> {
                val names = highShopping.map { it.title }.filter { it.isNotBlank() }.take(2)
                when (names.size) {
                    0 -> "Review your shopping list."
                    1 -> "${names[0]} is still unchecked."
                    else -> "${names[0]} and ${names[1]} are still unchecked."
                }
            }
            else -> "Open Tasks to review."
        }

        return ReminderInfo(title, detail)
    }
}
