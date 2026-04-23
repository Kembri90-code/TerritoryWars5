package com.territorywars.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun TerritoryLogo(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawHexLogo(cx, cy, w, h, color)
    }
}

private fun DrawScope.hexPoints(cx: Float, cy: Float, r: Float): List<Offset> =
    (0 until 6).map { i ->
        val angle = (i * 60 - 90) * PI / 180.0
        Offset(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
    }

private fun DrawScope.hexPath(points: List<Offset>): Path = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    close()
}

private fun DrawScope.drawHexLogo(cx: Float, cy: Float, w: Float, h: Float, color: Color) {
    val outerR = minOf(w, h) * 0.47f
    val innerR = outerR * 0.65f

    val outerPts = hexPoints(cx, cy, outerR)
    val innerPts = hexPoints(cx, cy, innerR)

    // Outer hex fill
    drawPath(hexPath(outerPts), color = color.copy(alpha = 0.08f))
    // Outer hex stroke
    drawPath(hexPath(outerPts), color = color, style = Stroke(width = w * 0.032f))

    // Inner hex fill
    drawPath(hexPath(innerPts), color = color.copy(alpha = 0.06f))
    // Inner hex stroke
    drawPath(hexPath(innerPts), color = color, style = Stroke(width = w * 0.019f))

    // Territory divider lines inside inner hex
    val strokeW = w * 0.016f
    drawLine(color.copy(alpha = 0.6f), innerPts[0], innerPts[3], strokeWidth = strokeW)
    drawLine(color.copy(alpha = 0.6f), innerPts[1], innerPts[4], strokeWidth = strokeW)
    drawLine(color.copy(alpha = 0.6f), innerPts[2], innerPts[5], strokeWidth = strokeW)

    // Spokes: outer vertex → inner vertex
    for (i in 0 until 6) {
        drawLine(color.copy(alpha = 0.7f), outerPts[i], innerPts[i], strokeWidth = strokeW * 1.2f)
    }

    // Center dot
    drawCircle(color, radius = w * 0.065f, center = Offset(cx, cy))
    // Center ring
    drawCircle(
        color.copy(alpha = 0.35f),
        radius = w * 0.13f,
        center = Offset(cx, cy),
        style = Stroke(width = w * 0.016f),
    )
}
