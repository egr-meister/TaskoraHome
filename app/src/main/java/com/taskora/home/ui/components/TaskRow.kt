package com.taskora.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskora.home.data.TaskPriority
import com.taskora.home.ui.theme.OverdueRed
import com.taskora.home.ui.theme.taskStatusColor
import com.taskora.home.util.TaskComputed
import com.taskora.home.util.categoryLabel
import com.taskora.home.util.displayDate
import com.taskora.home.util.recurrenceLabel

/**
 * Maintenance-specific task row. A colored status bar on the left encodes the
 * status (in addition to the text pill), the title and room sit at the top, and
 * schedule details fill the lower line.
 */
@Composable
fun MaintenanceTaskRow(
    computed: TaskComputed,
    roomName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val task = computed.task
    val statusColor = taskStatusColor(computed.status)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left status accent bar.
        Box(
            Modifier
                .width(5.dp)
                .height(64.dp)
                .background(statusColor)
        )
        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = roomName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "  •  ${categoryLabel(task.category)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.priority == TaskPriority.High) {
                    Icon(
                        imageVector = Icons.Filled.PriorityHigh,
                        contentDescription = "High priority",
                        tint = OverdueRed,
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                }
                Text(
                    text = task.title.ifBlank { "Untitled task" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            val nextText = computed.nextDue?.let { displayDate(it) } ?: "No date"
            val recurrence = recurrenceLabel(
                task.scheduleType, task.intervalValue, task.selectedMonths,
                task.yearlyMonth, task.yearlyDay
            )
            Text(
                text = "Next: $nextText  ·  $recurrence",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            computed.latestCompletion?.let {
                Text(
                    text = "Last done: ${displayDate(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            TaskStatusPill(computed.status)
        }
    }
}
