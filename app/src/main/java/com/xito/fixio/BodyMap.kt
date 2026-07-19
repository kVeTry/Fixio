package com.xito.fixio

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

data class DrawPoint(val x: Float, val y: Float, val score: Int, val back: Boolean)

private data class NZone(val id: String, val x: Float, val y: Float, val w: Float, val h: Float)

private val frontZones = listOf(
    NZone("cabeza", 118f, 6f, 64f, 62f),
    NZone("cuello", 128f, 68f, 44f, 22f),
    NZone("hombro_der", 76f, 90f, 48f, 32f),
    NZone("hombro_izq", 176f, 90f, 48f, 32f),
    NZone("pecho", 106f, 94f, 88f, 64f),
    NZone("abdomen", 110f, 158f, 80f, 68f),
    NZone("brazo_der", 60f, 122f, 36f, 72f),
    NZone("brazo_izq", 204f, 122f, 36f, 72f),
    NZone("codo_der", 56f, 194f, 36f, 30f),
    NZone("codo_izq", 208f, 194f, 36f, 30f),
    NZone("muneca_der", 48f, 258f, 34f, 30f),
    NZone("muneca_izq", 218f, 258f, 34f, 30f),
    NZone("cadera", 108f, 226f, 84f, 42f),
    NZone("muslo_der", 104f, 268f, 44f, 90f),
    NZone("muslo_izq", 152f, 268f, 44f, 90f),
    NZone("rodilla_der", 106f, 358f, 40f, 34f),
    NZone("rodilla_izq", 154f, 358f, 40f, 34f),
    NZone("tobillo_der", 110f, 496f, 34f, 30f),
    NZone("tobillo_izq", 156f, 496f, 34f, 30f),
    NZone("pie_der", 96f, 526f, 48f, 28f),
    NZone("pie_izq", 156f, 526f, 48f, 28f)
)

private val backZones = listOf(
    NZone("cabeza", 118f, 6f, 64f, 62f),
    NZone("nuca", 128f, 66f, 44f, 26f),
    NZone("hombro_izq", 76f, 90f, 48f, 32f),
    NZone("hombro_der", 176f, 90f, 48f, 32f),
    NZone("espalda_alta", 104f, 94f, 92f, 52f),
    NZone("espalda_media", 108f, 146f, 84f, 46f),
    NZone("lumbar", 110f, 192f, 80f, 42f),
    NZone("brazo_izq", 60f, 122f, 36f, 72f),
    NZone("brazo_der", 204f, 122f, 36f, 72f),
    NZone("gluteo_izq", 106f, 234f, 42f, 42f),
    NZone("gluteo_der", 152f, 234f, 42f, 42f),
    NZone("isquio_izq", 104f, 278f, 42f, 80f),
    NZone("isquio_der", 154f, 278f, 42f, 80f),
    NZone("gemelo_izq", 106f, 394f, 40f, 92f),
    NZone("gemelo_der", 154f, 394f, 40f, 92f),
    NZone("talon_izq", 110f, 516f, 32f, 32f),
    NZone("talon_der", 158f, 516f, 32f, 32f)
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

/** Silueta humana curva y suave, con degradado y proporciones naturales. */
private fun DrawScope.silhouette(scale: Float, female: Boolean, back: Boolean) {
    fun px(x: Float) = x * scale
    val grad = Brush.verticalGradient(
        listOf(Color(0xFF8B85FF).copy(alpha = 0.55f), Color(0xFF6C63FF).copy(alpha = 0.45f), Color(0xFF00BFA6).copy(alpha = 0.4f)),
        startY = 0f, endY = px(VH)
    )
    val body = Path().apply {
        // cabeza
        addOval(Rect(px(122f), px(4f), px(178f), px(68f)))
        // cuello
        moveTo(px(138f), px(64f)); lineTo(px(162f), px(64f))
        lineTo(px(164f), px(90f)); lineTo(px(136f), px(90f)); close()
        // torso con hombros redondeados y cintura
        moveTo(px(100f), px(104f))
        cubicTo(px(102f), px(88f), px(122f), px(86f), px(150f), px(86f))
        cubicTo(px(178f), px(86f), px(198f), px(88f), px(200f), px(104f))
        if (female) {
            cubicTo(px(202f), px(140f), px(184f), px(150f), px(182f), px(172f))
            cubicTo(px(180f), px(196f), px(200f), px(208f), px(198f), px(236f))
        } else {
            cubicTo(px(202f), px(150f), px(196f), px(180f), px(194f), px(236f))
        }
        cubicTo(px(192f), px(252f), px(168f), px(258f), px(150f), px(258f))
        cubicTo(px(132f), px(258f), px(108f), px(252f), px(102f), px(236f))
        if (female) {
            cubicTo(px(100f), px(208f), px(120f), px(196f), px(118f), px(172f))
            cubicTo(px(116f), px(150f), px(98f), px(140f), px(100f), px(104f))
        } else {
            cubicTo(px(104f), px(180f), px(98f), px(150f), px(100f), px(104f))
        }
        close()
        // brazo izquierdo (del espectador)
        moveTo(px(98f), px(100f))
        cubicTo(px(80f), px(108f), px(70f), px(140f), px(68f), px(176f))
        cubicTo(px(66f), px(210f), px(60f), px(240f), px(58f), px(268f))
        cubicTo(px(57f), px(284f), px(74f), px(288f), px(80f), px(276f))
        cubicTo(px(86f), px(244f), px(92f), px(212f), px(96f), px(180f))
        cubicTo(px(99f), px(152f), px(102f), px(124f), px(98f), px(100f))
        close()
        // brazo derecho
        moveTo(px(202f), px(100f))
        cubicTo(px(220f), px(108f), px(230f), px(140f), px(232f), px(176f))
        cubicTo(px(234f), px(210f), px(240f), px(240f), px(242f), px(268f))
        cubicTo(px(243f), px(284f), px(226f), px(288f), px(220f), px(276f))
        cubicTo(px(214f), px(244f), px(208f), px(212f), px(204f), px(180f))
        cubicTo(px(201f), px(152f), px(198f), px(124f), px(202f), px(100f))
        close()
        // pierna izquierda
        moveTo(px(106f), px(250f))
        cubicTo(px(100f), px(300f), px(104f), px(350f), px(110f), px(392f))
        cubicTo(px(114f), px(430f), px(112f), px(470f), px(114f), px(508f))
        cubicTo(px(115f), px(522f), px(138f), px(522f), px(140f), px(508f))
        cubicTo(px(142f), px(468f), px(144f), px(428f), px(146f), px(392f))
        cubicTo(px(148f), px(348f), px(148f), px(300f), px(148f), px(258f))
        close()
        // pierna derecha
        moveTo(px(194f), px(250f))
        cubicTo(px(200f), px(300f), px(196f), px(350f), px(190f), px(392f))
        cubicTo(px(186f), px(430f), px(188f), px(470f), px(186f), px(508f))
        cubicTo(px(185f), px(522f), px(162f), px(522f), px(160f), px(508f))
        cubicTo(px(158f), px(468f), px(156f), px(428f), px(154f), px(392f))
        cubicTo(px(152f), px(348f), px(152f), px(300f), px(152f), px(258f))
        close()
        // pies
        addOval(Rect(px(98f), px(516f), px(146f), px(548f)))
        addOval(Rect(px(154f), px(516f), px(202f), px(548f)))
    }
    drawPath(body, grad)
    drawPath(body, Color.White.copy(alpha = 0.25f), style = Stroke(2f * scale))
    // detalle: línea central de espalda
    if (back) {
        drawLine(
            Color.White.copy(alpha = 0.18f),
            Offset(px(150f), px(95f)), Offset(px(150f), px(240f)), 2.5f * scale
        )
    }
}

@Composable
fun BodyMap(
    modifier: Modifier,
    back: Boolean,
    female: Boolean,
    scores: Map<String, Int>,
    drawPoints: List<DrawPoint> = emptyList(),
    drawMode: Boolean = false,
    brushScore: Int = 5,
    onDraw: (Float, Float) -> Unit = { _, _ -> },
    onZoneTap: (String) -> Unit
) {
    val zones = if (back) backZones else frontZones
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.45f, 0.7f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pa"
    )
    Canvas(
        modifier
            .pointerInput(back, drawMode) {
                if (drawMode) {
                    detectDragGestures { change, _ ->
                        onDraw(change.position.x / size.width * VW, change.position.y / size.height * VH)
                    }
                } else {
                    detectTapGestures { pos ->
                        val scale = size.width / VW
                        val hit = zones.lastOrNull { z ->
                            Rect(z.x * scale, z.y * scale, (z.x + z.w) * scale, (z.y + z.h) * scale).contains(pos)
                        }
                        hit?.let { onZoneTap(it.id) }
                    }
                }
            }
            .pointerInput(drawMode) {
                if (drawMode) detectTapGestures { pos ->
                    onDraw(pos.x / size.width * VW, pos.y / size.height * VH)
                }
            }
    ) {
        val scale = size.width / VW
        silhouette(scale, female, back)
        // manchas suaves sobre zonas con dolor (sin recuadros)
        zones.forEach { z ->
            val s = scores[z.id] ?: 0
            if (s > 0) {
                val cx = (z.x + z.w / 2f) * scale
                val cy = (z.y + z.h / 2f) * scale
                val r = maxOf(z.w, z.h) * 0.75f * scale
                drawCircle(
                    Brush.radialGradient(
                        listOf(scoreColor(s).copy(alpha = pulse), scoreColor(s).copy(alpha = pulse * 0.4f), Color.Transparent),
                        Offset(cx, cy), r
                    ),
                    r, Offset(cx, cy)
                )
            }
        }
        // trazos dibujados por el usuario
        drawPoints.filter { it.back == back }.forEach { p ->
            val c = scoreColor(p.score)
            val r = 14f * scale
            drawCircle(
                Brush.radialGradient(listOf(c.copy(alpha = 0.8f), c.copy(alpha = 0.25f), Color.Transparent), Offset(p.x * scale, p.y * scale), r),
                r, Offset(p.x * scale, p.y * scale)
            )
        }
    }
}

fun bodyAspect(): Float = VW / VH
