package com.taskora.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.taskora.home.ui.theme.GoodGreen
import com.taskora.home.ui.theme.OverdueRed
import com.taskora.home.ui.theme.SoonAmber
import com.taskora.home.util.CalendarUtils
import com.taskora.home.util.DayMarkers
import com.taskora.home.util.displayMonthYear
import java.time.LocalDate
import java.time.YearMonth
import com.taskora.home.data.WeekDay

/** In-app monthly calendar with accessible due / overdue / completion markers. */
@Composable
fun CalendarView(
    yearMonth: YearMonth,
    firstDayOfWeek: WeekDay,
    markers: Map<LocalDate, DayMarkers>,
    selectedDate: LocalDate?,
    todayDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = displayMonthYear(yearMonth.atDay(1)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Row(Modifier.fillMaxWidth()) {
            CalendarUtils.weekdayHeaders(firstDayOfWeek).forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        val cells = CalendarUtils.monthGrid(yearMonth, firstDayOfWeek)
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    DayCell(
                        date = cell.date,
                        inMonth = cell.inCurrentMonth,
                        isToday = cell.date == todayDate,
                        isSelected = cell.date == selectedDate,
                        markers = markers[cell.date],
                        onClick = { onSelectDate(cell.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    markers: DayMarkers?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseTextColor = if (inMonth) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    val selectedBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    else Color.Transparent

    val a11y = "${date.dayOfMonth}, ${markers?.accessibilityLabel() ?: "no maintenance"}"

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(selectedBg)
            .then(
                if (isToday) Modifier.border(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = a11y },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = baseTextColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (markers?.overdue == true) MarkerDot(OverdueRed)
                if (markers?.dueSoon == true) MarkerDot(SoonAmber)
                if (markers?.hasCompletion == true) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = GoodGreen,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkerDot(color: Color) {
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}
