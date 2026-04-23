package com.territorywars.presentation.map

import android.graphics.*
import kotlin.math.*

enum class PlayerMarker(val id: String, val labelRu: String) {
    DOT("dot", "Точка"),
    ARROW("arrow", "Стрелка"),
    STAR("star", "Звезда"),
    DIAMOND("diamond", "Ромб")
}

fun playerMarkerById(id: String?): PlayerMarker =
    PlayerMarker.entries.firstOrNull { it.id == id } ?: PlayerMarker.DOT

fun createMarkerBitmap(marker: PlayerMarker, colorHex: String): Bitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f

    val baseColor = try { Color.parseColor(colorHex) } catch (_: Exception) { 0xFF2979FF.toInt() }

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = baseColor
        style = Paint.Style.FILL
    }
    val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    val whiteStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    when (marker) {
        PlayerMarker.DOT -> {
            canvas.drawCircle(cx, cy, 44f, whiteFill)
            canvas.drawCircle(cx, cy, 36f, fillPaint)
            canvas.drawCircle(cx, cy, 14f, whiteFill)
        }
        PlayerMarker.ARROW -> {
            canvas.drawCircle(cx, cy, 44f, whiteFill)
            val path = Path().apply {
                moveTo(cx, 8f)
                lineTo(cx + 28f, cy + 26f)
                lineTo(cx, cy + 12f)
                lineTo(cx - 28f, cy + 26f)
                close()
            }
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, whiteStroke)
        }
        PlayerMarker.STAR -> {
            canvas.drawCircle(cx, cy, 44f, whiteFill)
            val path = Path()
            val outerR = 36f
            val innerR = 16f
            for (i in 0 until 10) {
                val angle = (i * 36 - 90) * PI / 180.0
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + r * cos(angle).toFloat()
                val y = cy + r * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, whiteStroke)
        }
        PlayerMarker.DIAMOND -> {
            canvas.drawCircle(cx, cy, 44f, whiteFill)
            val path = Path().apply {
                moveTo(cx, cy - 36f)
                lineTo(cx + 28f, cy)
                lineTo(cx, cy + 36f)
                lineTo(cx - 28f, cy)
                close()
            }
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, whiteStroke)
        }
    }

    return bitmap
}
