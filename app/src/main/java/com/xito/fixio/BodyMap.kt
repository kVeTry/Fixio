package com.xito.fixio

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable

// Zonas en coordenadas normalizadas (0..1) sobre lienzo 300x600
private data class NZone(val id: String, val x: Float, val y: Float, val w: Float, val h: Float, val oval: Boolean = false)

private val frontZones = listOf(
    NZone("cabeza", 120f, 8f, 60f, 58f, true),
    NZone("cuello", 130f, 66f, 40f, 24f),
    NZone("hombro_der", 78f, 92f, 46f, 30f, true),
    NZone("hombro_izq", 176f, 92f, 46f, 30f, true),
    NZone("pecho", 108f, 96f, 84f, 62f),
    NZone("abdomen", 112f, 160f, 76f, 66f),
    NZone("brazo_der", 62f, 122f, 34f, 70f),
    NZone("brazo_izq", 204f, 122f, 34f, 70f),
    NZone("codo_der", 58f, 194f, 34f, 28f, true),
    NZone("codo_izq", 208f, 194f, 34f, 28f, true),
    NZone("muneca_der", 50f, 262f, 32f, 26f, true),
    NZone("muneca_izq", 218f, 262f, 32f, 26f, true),
    NZone("cadera", 110f, 228f, 80f, 40f),
    NZone("muslo_der", 106f, 270f, 40f, 88f),
    NZone("muslo_izq", 154f, 270f, 40f, 88f),
    NZone("rodilla_der", 108f, 360f, 36f, 32f, true),
    NZone("rodilla_izq", 156f, 360f, 36f, 32f, true),
    NZone("tobillo_der", 112f, 500f, 30f, 26f, true),
    NZone("tobillo_izq", 158f, 500f, 30f, 26f, true),
    NZone("pie_der", 100f, 528f, 44f, 26f),
    NZone("pie_izq", 156f, 528f, 44f, 26f)
)

private val backZones = listOf(
    NZone("cabeza", 120f, 8f, 60f, 58f, true),
    NZone("nuca", 130f, 66f, 40f, 26f),
    NZone("hombro_izq", 78f, 92f, 46f, 30f, true),
    NZone("hombro_der", 176f, 92f, 46f, 30f, true),
    NZone("espalda_alta", 106f, 96f, 88f, 50f),
    NZone("espalda_media", 110f, 148f, 80f, 44f),
    NZone("lumbar", 112f, 194f, 76f, 40f),
    NZone("brazo_izq", 62f, 122f, 34f, 70f),
    NZone("brazo_der", 204f, 122f, 34f, 70f),
    NZone("gluteo_izq", 108f, 236f, 40f, 40f, true),
    NZone("gluteo_der", 152f, 236f, 40f, 40f, true),
    NZone("isquio_izq", 106f, 280f, 40f, 78f),
    NZone("isquio_der", 154f, 280f, 40f, 78f),
    NZone("gemelo_izq", 108f, 396f, 36f, 90f),
    NZone("gemelo_der", 156f, 396f, 36f, 90f),
    NZone("talon_izq", 112f, 520f, 30f, 30f, true),
    NZone("talon_der", 158f, 520f, 30f, 30f, true)
)

private const val VW = 300f
private const val VH = 600f

fun scoreColor(score: Int): Color = when {
    score >= 8 -> Color(0xFFE53935)
    score >= 6 -> Color(0xFFF4511E)
    score >= 4 -> Color(0xFFFFB300)
    score >= 1 -> Color(0xFF7CB342)
    else -> Color.Transparent
}

private fun DrawScope.silhouette(scale: Float, female: Boolean, base: Color) {
    fun rr(x: Float, y: Float, w: Float, h: Float, r: Float = 14f) =
        drawRoundRect(base, Offset(x * scale, y * scale), Size(w * scale, h * scale), CornerRadius(r * scale))
    fun ov(x: Float, y: Float, w: Float, h: Float) =
        drawOval(base, Offset(x * scale, y * scale), Size(w * scale, h * scale))
    // cabeza y cuello
    ov(122f, 6f, 56f, 62f)
    rr(136f, 64f, 28f, 26f, 8f)
    // torso (con cintura marcada si female)
    if (female) {
        rr(104f, 88f, 92f, 60f, 26f)
        rr(116f, 146f, 68f, 50f, 22f)
        rr(104f, 194f, 92f, 56f, 30f)
    } else {
        rr(100f, 88f, 100f, 110f, 24f)
        rr(106f, 196f, 88f, 54f, 22f)
    }
    // brazos
    rr(64f, 96f, 32f, 176f, 16f); rr(204f, 96f, 32f, 176f, 16f)
    // manos
    ov(58f, 274f, 30f, 34f); ov(212f, 274f, 30f, 34f)
    // piernas
    rr(106f, 250f, 40f, 250f, 18f); rr(154f, 250f, 40f, 250f, 18f)
    // pies
    rr(96f, 526f, 48f, 26f, 12f); rr(156f, 526f, 48f, 26f, 12f)
}

@Composable
fun BodyMap(
    modifier: Modifier,
    back: Boolean,
    female: Boolean,
    scores: Map<String, Int>,
    onZoneTap: (String) -> Unit
) {
    val zones = if (back) backZones else frontZones
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        )
    )
    Canvas(
        modifier.pointerInput(back) {
            detectTapGestures { pos ->
                val scale = size.width / VW
                val hit = zones.lastOrNull { z ->
                    Rect(z.x * scale, z.y * scale, (z.x + z.w) * scale, (z.y + z.h) * scale).contains(pos)
                }
                hit?.let { onZoneTap(it.id) }
            }
        }
    ) {
        val scale = size.width / VW
        silhouette(scale, female, Color(0xFF9E9AC8).copy(alpha = 0.35f))
        zones.forEach { z ->
            val s = scores[z.id] ?: 0
            val fill = if (s > 0) scoreColor(s).copy(alpha = pulse) else Color.White.copy(alpha = 0.06f)
            val tl = Offset(z.x * scale, z.y * scale)
            val sz = Size(z.w * scale, z.h * scale)
            if (z.oval) {
                drawOval(fill, tl, sz)
                drawOval(Color.White.copy(alpha = 0.35f), tl, sz, style = Stroke(1.5f * scale))
            } else {
                drawRoundRect(fill, tl, sz, CornerRadius(10f * scale))
                drawRoundRect(Color.White.copy(alpha = 0.35f), tl, sz, CornerRadius(10f * scale), style = Stroke(1.5f * scale))
            }
        }
    }
}

fun bodyAspect(): Float = VW / VH
