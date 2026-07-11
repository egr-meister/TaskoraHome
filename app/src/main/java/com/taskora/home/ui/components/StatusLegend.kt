package com.taskora.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.taskora.home.ui.theme.GoodGreen
import com.taskora.home.ui.theme.NoTasksGray
import com.taskora.home.ui.theme.OverdueRed
import com.taskora.home.ui.theme.SoonAmber

/** Horizontal status legend for the home map: Good, Soon, Overdue, No Tasks. */
@Composable
fun StatusLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Legend: green means good, amber means due soon, red means overdue, gray means no tasks."
            },
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LegendEntry(GoodGreen, "Good")
        LegendEntry(SoonAmber, "Soon")
        LegendEntry(OverdueRed, "Overdue")
        LegendEntry(NoTasksGray, "No Tasks")
    }
}

@Composable
private fun LegendEntry(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color = color, size = 9)
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
