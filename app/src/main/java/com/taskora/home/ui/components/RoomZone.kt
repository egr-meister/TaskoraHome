package com.taskora.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskora.home.data.ZoneStatus
import com.taskora.home.ui.theme.zoneStatusColor
import com.taskora.home.util.zoneStatusLabel

/** Data used to render a single room zone on the home map. */
data class RoomZoneData(
    val roomId: String,
    val name: String,
    val accent: Color,
    val status: ZoneStatus,
    val activeCount: Int,
    val overdueCount: Int,
    val soonCount: Int,
    val nearestDue: String?
)

/**
 * A single room cell in the cutaway house map. The left accent stripe is the
 * room color; the status is shown as a colored top border + text pill so status
 * is never conveyed by color alone.
 */
@Composable
fun RoomZoneCell(
    data: RoomZoneData,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = zoneStatusColor(data.status)
    val borderColor = if (highlighted) statusColor else MaterialTheme.colorScheme.outline
    val borderWidth = if (highlighted) 2.dp else 1.dp

    val a11y = buildString {
        append(data.name)
        append(", status ${zoneStatusLabel(data.status)}")
        append(", ${data.activeCount} active tasks")
        if (data.overdueCount > 0) append(", ${data.overdueCount} overdue")
        if (data.soonCount > 0) append(", ${data.soonCount} due soon")
        data.nearestDue?.let { append(", next $it") }
    }

    Column(
        modifier = modifier
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = a11y }
    ) {
        // Room accent stripe.
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(data.accent)
        )
        Column(Modifier.padding(10.dp)) {
            Text(
                text = data.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            StatusPill(zoneStatusLabel(data.status), statusColor)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountTag("${data.activeCount} tasks")
                if (data.overdueCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    CountTag("${data.overdueCount} overdue", zoneStatusColor(ZoneStatus.Overdue))
                } else if (data.soonCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    CountTag("${data.soonCount} soon", zoneStatusColor(ZoneStatus.Soon))
                }
            }
            data.nearestDue?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Next: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CountTag(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
