package com.taskora.home.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.taskora.home.ui.theme.HouseNavy
import com.taskora.home.ui.theme.KitchenAmber
import com.taskora.home.ui.theme.WarmSand

/**
 * A small decorative cutaway house drawn entirely with Compose shapes — no
 * external images. Used on onboarding and empty states. It depicts a navy roof,
 * three room zones, and one amber maintenance dot.
 */
@Composable
fun HouseIllustration(modifier: Modifier = Modifier, sizeDp: Int = 120) {
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val w = size.width
        val h = size.height

        // Roof
        val roof = Path().apply {
            moveTo(w * 0.10f, h * 0.42f)
            lineTo(w * 0.50f, h * 0.08f)
            lineTo(w * 0.90f, h * 0.42f)
            close()
        }
        drawPath(roof, HouseNavy)

        // Body
        val bodyTopLeft = Offset(w * 0.16f, h * 0.42f)
        val bodySize = Size(w * 0.68f, h * 0.50f)
        drawRect(color = WarmSand, topLeft = bodyTopLeft, size = bodySize)

        // Body outline (navy stroke via thin rects)
        val stroke = w * 0.012f
        drawRect(HouseNavy, bodyTopLeft, Size(bodySize.width, stroke))
        drawRect(HouseNavy, Offset(bodyTopLeft.x, bodyTopLeft.y + bodySize.height - stroke), Size(bodySize.width, stroke))
        drawRect(HouseNavy, bodyTopLeft, Size(stroke, bodySize.height))
        drawRect(HouseNavy, Offset(bodyTopLeft.x + bodySize.width - stroke, bodyTopLeft.y), Size(stroke, bodySize.height))

        // Interior dividers -> three zones
        val midX = w * 0.50f
        drawRect(HouseNavy, Offset(midX - stroke / 2, bodyTopLeft.y), Size(stroke, bodySize.height * 0.55f))
        val midY = bodyTopLeft.y + bodySize.height * 0.55f
        drawRect(HouseNavy, Offset(bodyTopLeft.x, midY), Size(bodySize.width, stroke))

        // One amber maintenance dot in the lower zone
        drawCircle(
            color = KitchenAmber,
            radius = w * 0.045f,
            center = Offset(w * 0.5f, bodyTopLeft.y + bodySize.height * 0.78f)
        )
    }
}
