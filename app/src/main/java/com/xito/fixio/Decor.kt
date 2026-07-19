package com.xito.fixio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

fun Modifier.graphicsLayerCompat(alpha: Float, translationY: Float): Modifier =
    this.graphicsLayer(alpha = alpha, translationY = translationY)

/** Fondo con blobs de degradado suaves que se desplazan lentamente. */
@Composable
fun AnimatedBackground(dark: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "p")
    val p2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "p2")

    val c1 = if (dark) Color(0xFF6C63FF) else Color(0xFF8B85FF)
    val c2 = if (dark) Color(0xFF00BFA6) else Color(0xFF4ED8C4)
    val c3 = if (dark) Color(0xFFB47CFF) else Color(0xFFFFB4E0)
    val a = if (dark) 0.18f else 0.28f

    Canvas(modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        fun blob(cx: Float, cy: Float, r: Float, col: Color) {
            drawCircle(
                Brush.radialGradient(listOf(col.copy(alpha = a), Color.Transparent), Offset(cx, cy), r),
                r, Offset(cx, cy)
            )
        }
        blob(w * (0.25f + 0.15f * sin(p * 6.28f)), h * (0.15f + 0.08f * cos(p * 6.28f)), w * 0.7f, c1)
        blob(w * (0.85f + 0.1f * cos(p2 * 6.28f)), h * (0.35f + 0.1f * sin(p2 * 6.28f)), w * 0.6f, c2)
        blob(w * (0.5f + 0.12f * sin(p2 * 3.14f)), h * (0.9f + 0.06f * cos(p * 6.28f)), w * 0.8f, c3)
    }
}

/** Degradado animado (barrido) para tarjetas hero. */
@Composable
fun animatedHeroBrush(): Brush {
    val t = rememberInfiniteTransition(label = "hero")
    val x by t.animateFloat(0f, 1000f, infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "x")
    return Brush.linearGradient(
        listOf(Color(0xFF6C63FF), Color(0xFF8B5CF6), Color(0xFF00BFA6)),
        start = Offset(x, 0f), end = Offset(x + 600f, 600f)
    )
}

/** Entrada suave: fade + slide hacia arriba, con retardo por índice para efecto escalonado. */
@Composable
fun SlideFadeIn(modifier: Modifier = Modifier, index: Int = 0, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val off by animateFloatAsState(
        if (shown) 0f else 40f,
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "off"
    )
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(400, index * 60), label = "al")
    Box(modifier.graphicsLayerCompat(alpha, off)) { content() }
}

/** Contador que sube animado hasta el objetivo. */
@Composable
fun animatedInt(target: Int): Int {
    val v by animateIntAsState(target, tween(900, easing = FastOutSlowInEasing), label = "int")
    return v
}

@Composable
fun animatedFloat1(target: Float): Float {
    val v by animateFloatAsState(target, tween(900, easing = FastOutSlowInEasing), label = "f1")
    return v
}

/** Ilustración vectorial suave para onboarding / estados vacíos. */
@Composable
fun WellnessArt(modifier: Modifier, primary: Color, secondary: Color) {
    val t = rememberInfiniteTransition(label = "art")
    val bob by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val cx = w / 2f
        // halo suave
        drawCircle(Brush.radialGradient(listOf(primary.copy(alpha = 0.22f), Color.Transparent), Offset(cx, h * 0.5f), w * 0.5f), w * 0.5f, Offset(cx, h * 0.5f))
        // arco energético
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.7f)
            quadraticBezierTo(cx, h * 0.25f + bob, w * 0.8f, h * 0.7f)
        }
        drawPath(path, secondary.copy(alpha = 0.5f), style = Stroke(width = 10f))
        // figura abstracta (persona estirando)
        val s = w / 200f
        val col = primary
        val yb = bob
        drawCircle(col, 15f * s, Offset(cx, h * 0.42f + yb))               // cabeza
        val body = Path().apply {
            moveTo(cx, h * 0.42f + 16f * s + yb)
            lineTo(cx, h * 0.62f + yb)
        }
        drawPath(body, col, style = Stroke(12f * s))
        // brazos hacia arriba
        drawPath(Path().apply { moveTo(cx, h * 0.5f + yb); lineTo(cx - 34f * s, h * 0.4f + yb) }, col, style = Stroke(10f * s))
        drawPath(Path().apply { moveTo(cx, h * 0.5f + yb); lineTo(cx + 34f * s, h * 0.4f + yb) }, col, style = Stroke(10f * s))
        // piernas
        drawPath(Path().apply { moveTo(cx, h * 0.62f + yb); lineTo(cx - 24f * s, h * 0.75f + yb) }, col, style = Stroke(10f * s))
        drawPath(Path().apply { moveTo(cx, h * 0.62f + yb); lineTo(cx + 24f * s, h * 0.75f + yb) }, col, style = Stroke(10f * s))
        // puntos decorativos
        drawCircle(secondary.copy(alpha = 0.7f), 6f * s, Offset(w * 0.2f, h * 0.35f))
        drawCircle(primary.copy(alpha = 0.6f), 5f * s, Offset(w * 0.82f, h * 0.3f))
        drawCircle(secondary.copy(alpha = 0.5f), 4f * s, Offset(w * 0.7f, h * 0.8f))
    }
}

/** Ilustración compacta para estados vacíos. */
@Composable
fun EmptyArt(modifier: Modifier, primary: Color, secondary: Color) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawCircle(primary.copy(alpha = 0.12f), w * 0.32f, Offset(w / 2, h / 2))
        drawCircle(secondary.copy(alpha = 0.5f), 8f, Offset(w * 0.4f, h * 0.42f))
        drawCircle(secondary.copy(alpha = 0.5f), 8f, Offset(w * 0.6f, h * 0.42f))
        drawPath(Path().apply {
            moveTo(w * 0.4f, h * 0.6f)
            quadraticBezierTo(w / 2, h * 0.68f, w * 0.6f, h * 0.6f)
        }, primary, style = Stroke(6f))
    }
}
