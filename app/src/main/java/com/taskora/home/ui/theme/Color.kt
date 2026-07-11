package com.taskora.home.ui.theme

import androidx.compose.ui.graphics.Color
import com.taskora.home.data.TaskStatus
import com.taskora.home.data.ZoneStatus

// ---- Structural palette ----------------------------------------------------
val HouseNavy = Color(0xFF29445D)
val DeepNavy = Color(0xFF1C3245)
val WarmSand = Color(0xFFE8D9BD)
val LightSand = Color(0xFFF6EFE3)

// ---- Room accents ----------------------------------------------------------
val KitchenAmber = Color(0xFFDFA33A)
val BathroomAqua = Color(0xFF55A6B3)
val BedroomLavender = Color(0xFF8A82B8)
val LivingTerracotta = Color(0xFFC9795C)
val LaundryBlueGray = Color(0xFF6F8FA3)
val GarageSlate = Color(0xFF68727B)
val OutdoorGreen = Color(0xFF6F9B68)
val UtilityBronze = Color(0xFFA17B55)

// ---- Status ----------------------------------------------------------------
val GoodGreen = Color(0xFF3B8D61)
val SoonAmber = Color(0xFFD1912E)
val OverdueRed = Color(0xFFB94A48)
val NoTasksGray = Color(0xFF8C949A)
val DisabledGray = Color(0xFFB5BABD)

// ---- Neutrals --------------------------------------------------------------
val AppBackground = Color(0xFFF4F2ED)
val SurfaceWhite = Color(0xFFFFFFFF)
val DeepText = Color(0xFF22282D)
val SecondaryText = Color(0xFF687078)
val DividerColor = Color(0xFFD8D4CC)

/** Maps a stored room colorKey to its accent color. */
fun roomAccent(colorKey: String): Color = when (colorKey.lowercase()) {
    "kitchen" -> KitchenAmber
    "bathroom" -> BathroomAqua
    "bedroom" -> BedroomLavender
    "living" -> LivingTerracotta
    "laundry" -> LaundryBlueGray
    "garage" -> GarageSlate
    "outdoor" -> OutdoorGreen
    "utility" -> UtilityBronze
    else -> UtilityBronze
}

/** Available color choices for the Add/Edit Room screen. */
val roomColorChoices: List<Pair<String, Color>> = listOf(
    "kitchen" to KitchenAmber,
    "bathroom" to BathroomAqua,
    "bedroom" to BedroomLavender,
    "living" to LivingTerracotta,
    "laundry" to LaundryBlueGray,
    "garage" to GarageSlate,
    "outdoor" to OutdoorGreen,
    "utility" to UtilityBronze
)

fun zoneStatusColor(status: ZoneStatus): Color = when (status) {
    ZoneStatus.Good -> GoodGreen
    ZoneStatus.Soon -> SoonAmber
    ZoneStatus.Overdue -> OverdueRed
    ZoneStatus.NoTasks -> NoTasksGray
}

fun taskStatusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.Good -> GoodGreen
    TaskStatus.Soon -> SoonAmber
    TaskStatus.Overdue -> OverdueRed
    TaskStatus.Completed -> GoodGreen
    TaskStatus.Unscheduled -> NoTasksGray
    TaskStatus.Disabled -> DisabledGray
    TaskStatus.InvalidSchedule -> NoTasksGray
}
