package com.xito.fixio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun LineChart(modifier: Modifier, values: List<Float>, maxY: Float, color: Color, grid: Color) {
    val anim = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { anim.snapTo(0f); anim.animateTo(1f, tween(900)) }
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val a = anim.value
        val w = size.width; val h = size.height
        for (i in 0..4) {
            val y = h * i / 4f
            drawLine(grid, Offset(0f, y), Offset(w, y), 1.5f)
        }
        val step = if (values.size > 1) w / (values.size - 1) else 0f
        val path = Path(); val fill = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = h - (v / maxY).coerceIn(0f, 1f) * h * a
            if (i == 0) { path.moveTo(x, y); fill.moveTo(x, h); fill.lineTo(x, y) }
            else { path.lineTo(x, y); fill.lineTo(x, y) }
        }
        fill.lineTo((values.size - 1) * step, h); fill.close()
        drawPath(fill, color.copy(alpha = 0.15f * a))
        drawPath(path, color, style = Stroke(5f))
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = h - (v / maxY).coerceIn(0f, 1f) * h * a
            drawCircle(color, 7f, Offset(x, y))
        }
    }
}

@Composable
fun BarChart(modifier: Modifier, values: List<Float>, maxY: Float, color: Color) {
    val anim = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { anim.snapTo(0f); anim.animateTo(1f, tween(900)) }
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val a = anim.value
        val w = size.width; val h = size.height
        val bw = w / values.size * 0.6f
        val gap = w / values.size
        values.forEachIndexed { i, v ->
            val bh = (v / maxY).coerceIn(0f, 1f) * h * a
            drawRoundRect(color, Offset(i * gap + (gap - bw) / 2f, h - bh), Size(bw, bh), CornerRadius(8f))
        }
    }
}
