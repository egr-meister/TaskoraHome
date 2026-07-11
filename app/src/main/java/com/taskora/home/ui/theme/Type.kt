package com.taskora.home.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 system typography with light weight tuning. No custom font files
 * are bundled — the default platform font family is used everywhere.
 */
private val defaultTypography = Typography()

val TaskoraTypography = Typography(
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = FontFamily.Default),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = FontFamily.Default),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = FontFamily.Default),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = FontFamily.Default),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = FontFamily.Default),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = FontFamily.Default),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = FontFamily.Default),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = FontFamily.Default),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)
