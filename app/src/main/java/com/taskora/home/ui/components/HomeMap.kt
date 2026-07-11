package com.taskora.home.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskora.home.data.ZoneStatus
import com.taskora.home.ui.theme.HouseNavy
import com.taskora.home.ui.theme.WarmSand
import com.taskora.home.ui.theme.zoneStatusColor
import com.taskora.home.util.zoneStatusLabel

/**
 * The "Home Maintenance Map": a simplified cutaway house. A drawn navy roof
 * sits above a sand-toned house body that holds the Whole-Home strip and a
 * deterministic two-column grid of room zones.
 *
 * This is an organizational visual, not an architectural plan.
 */
@Composable
fun HomeMap(
    wholeHomeStatus: ZoneStatus,
    wholeHomeActive: Int,
    wholeHomeOverdue: Int,
    wholeHomeNextTitle: String?,
    rooms: List<RoomZoneData>,
    highlightRoomId: String?,
    onWholeHomeClick: () -> Unit,
    onRoomClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Roof.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 8.dp)
        ) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.06f, h)
                lineTo(w * 0.5f, 0f)
                lineTo(w * 0.94f, h)
                close()
            }
            drawPath(path = path, color = HouseNavy)
        }

        // House body.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(WarmSand.copy(alpha = 0.45f))
                .border(
                    width = 1.5.dp,
                    color = HouseNavy.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WholeHomeStrip(
                status = wholeHomeStatus,
                activeCount = wholeHomeActive,
                overdueCount = wholeHomeOverdue,
                nextTitle = wholeHomeNextTitle,
                onClick = onWholeHomeClick
            )

            if (rooms.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No rooms yet. Add rooms to build your maintenance map.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                rooms.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { room ->
                            RoomZoneCell(
                                data = room,
                                highlighted = room.roomId == highlightRoomId,
                                onClick = { onRoomClick(room.roomId) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Keep the grid aligned when a row has a single cell.
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun WholeHomeStrip(
    status: ZoneStatus,
    activeCount: Int,
    overdueCount: Int,
    nextTitle: String?,
    onClick: () -> Unit
) {
    val statusColor = zoneStatusColor(status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = null,
            tint = HouseNavy
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Whole Home",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = buildString {
                    append("$activeCount active")
                    if (overdueCount > 0) append(" · $overdueCount overdue")
                    nextTitle?.let { append(" · next: $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        StatusPill(zoneStatusLabel(status), statusColor)
    }
}
