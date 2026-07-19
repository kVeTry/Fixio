package com.xito.fixio

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import kotlin.math.cos
import kotlin.math.sin

/** Mantiene la pantalla encendida mientras el composable está visible. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

enum class AnimKind { NECK, SHOULDER, BACK_CAT, SQUAT, STRETCH_UP, CALF, WRIST, HIP, GENERIC }

fun animKindFor(zoneId: String): AnimKind = when {
    zoneId in listOf("cuello", "nuca", "cabeza") -> AnimKind.NECK
    zoneId.startsWith("hombro") -> AnimKind.SHOULDER
    zoneId.startsWith("espalda") || zoneId == "lumbar" -> AnimKind.BACK_CAT
    zoneId.startsWith("rodilla") || zoneId.startsWith("muslo") -> AnimKind.SQUAT
    zoneId.startsWith("gemelo") || zoneId.startsWith("tobillo") || zoneId.startsWith("pie") || zoneId.startsWith("talon") -> AnimKind.CALF
    zoneId.startsWith("muneca") || zoneId.startsWith("codo") || zoneId.startsWith("brazo") -> AnimKind.WRIST
    zoneId == "cadera" || zoneId.startsWith("gluteo") || zoneId.startsWith("isquio") -> AnimKind.HIP
    else -> AnimKind.GENERIC
}

private fun DrawScope.limb(a: Offset, b: Offset, c: Color, w: Float) =
    drawLine(c, a, b, w, cap = StrokeCap.Round)

/** Figura animada que demuestra el tipo de ejercicio, en bucle suave. */
@Composable
fun ExerciseAnim(modifier: Modifier, kind: AnimKind, color: Color, accent: Color) {
    val t = rememberInfiniteTransition(label = "ex")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")

    Canvas(modifier) {
        val w = size.width; val h = size.height
        val s = w / 200f
        val lw = 9f * s
        val cx = w / 2f
        // suelo sutil
        drawLine(accent.copy(alpha = 0.3f), Offset(w * 0.15f, h * 0.88f), Offset(w * 0.85f, h * 0.88f), 3f * s, cap = StrokeCap.Round)

        when (kind) {
            AnimKind.NECK -> {
                val tilt = (p - 0.5f) * 0.9f
                val neck = Offset(cx, h * 0.42f)
                val head = Offset(cx + sin(tilt) * 26f * s, h * 0.42f - cos(tilt) * 30f * s)
                drawCircle(color, 16f * s, head)
                limb(neck, Offset(cx, h * 0.66f), color, lw)
                limb(Offset(cx, h * 0.47f), Offset(cx - 30f * s, h * 0.6f), color, lw)
                limb(Offset(cx, h * 0.47f), Offset(cx + 30f * s, h * 0.6f), color, lw)
                limb(Offset(cx, h * 0.66f), Offset(cx - 16f * s, h * 0.88f), color, lw)
                limb(Offset(cx, h * 0.66f), Offset(cx + 16f * s, h * 0.88f), color, lw)
                limb(neck, head, color, lw)
            }
            AnimKind.SHOULDER -> {
                val ang = p * 6.28f
                val sh = Offset(cx, h * 0.5f)
                drawCircle(color, 15f * s, Offset(cx, h * 0.36f))
                limb(Offset(cx, h * 0.42f), Offset(cx, h * 0.68f), color, lw)
                limb(Offset(cx, h * 0.68f), Offset(cx - 15f * s, h * 0.88f), color, lw)
                limb(Offset(cx, h * 0.68f), Offset(cx + 15f * s, h * 0.88f), color, lw)
                // brazo circular
                val hand = Offset(sh.x + cos(ang) * 40f * s, sh.y + sin(ang) * 40f * s)
                limb(sh, hand, accent, lw)
                drawCircle(accent.copy(alpha = 0.25f), 42f * s, sh, style = Stroke(2.5f * s))
                limb(sh, Offset(cx - 34f * s, h * 0.62f), color, lw)
            }
            AnimKind.BACK_CAT -> {
                val arch = (p - 0.5f) * 36f * s
                val hip = Offset(cx + 44f * s, h * 0.62f)
                val shoulder = Offset(cx - 44f * s, h * 0.62f)
                val mid = Offset(cx, h * 0.62f - arch)
                val spine = Path().apply {
                    moveTo(shoulder.x, shoulder.y)
                    quadraticBezierTo(mid.x, mid.y, hip.x, hip.y)
                }
                drawPath(spine, color, style = Stroke(lw, cap = StrokeCap.Round))
                drawCircle(color, 13f * s, Offset(shoulder.x - 14f * s, shoulder.y - 6f * s + arch * 0.3f))
                limb(shoulder, Offset(shoulder.x, h * 0.88f), color, lw)
                limb(hip, Offset(hip.x, h * 0.88f), color, lw)
            }
            AnimKind.SQUAT -> {
                val d = p * 30f * s
                drawCircle(color, 15f * s, Offset(cx, h * 0.3f + d))
                limb(Offset(cx, h * 0.36f + d), Offset(cx, h * 0.56f + d), color, lw)
                limb(Offset(cx, h * 0.4f + d), Offset(cx - 32f * s, h * 0.44f + d), color, lw)
                limb(Offset(cx, h * 0.4f + d), Offset(cx + 32f * s, h * 0.44f + d), color, lw)
                limb(Offset(cx, h * 0.56f + d), Offset(cx - 20f * s, h * 0.72f), color, lw)
                limb(Offset(cx - 20f * s, h * 0.72f), Offset(cx - 20f * s, h * 0.88f), color, lw)
                limb(Offset(cx, h * 0.56f + d), Offset(cx + 20f * s, h * 0.72f), color, lw)
                limb(Offset(cx + 20f * s, h * 0.72f), Offset(cx + 20f * s, h * 0.88f), color, lw)
            }
            AnimKind.STRETCH_UP, AnimKind.GENERIC -> {
                val up = p * 14f * s
                drawCircle(color, 15f * s, Offset(cx, h * 0.34f - up * 0.3f))
                limb(Offset(cx, h * 0.4f), Offset(cx, h * 0.66f), color, lw)
                limb(Offset(cx, h * 0.45f), Offset(cx - 26f * s, h * 0.28f - up), accent, lw)
                limb(Offset(cx, h * 0.45f), Offset(cx + 26f * s, h * 0.28f - up), accent, lw)
                limb(Offset(cx, h * 0.66f), Offset(cx - 16f * s, h * 0.88f), color, lw)
                limb(Offset(cx, h * 0.66f), Offset(cx + 16f * s, h * 0.88f), color, lw)
            }
            AnimKind.CALF -> {
                val rise = p * 12f * s
                drawCircle(color, 14f * s, Offset(cx, h * 0.3f - rise))
                limb(Offset(cx, h * 0.36f - rise), Offset(cx, h * 0.6f - rise), color, lw)
                limb(Offset(cx, h * 0.42f - rise), Offset(cx - 26f * s, h * 0.36f - rise), color, lw)
                limb(Offset(cx, h * 0.42f - rise), Offset(cx + 26f * s, h * 0.36f - rise), color, lw)
                limb(Offset(cx, h * 0.6f - rise), Offset(cx - 12f * s, h * 0.8f - rise), color, lw)
                limb(Offset(cx, h * 0.6f - rise), Offset(cx + 12f * s, h * 0.8f - rise), color, lw)
                // talones elevándose
                limb(Offset(cx - 12f * s, h * 0.8f - rise), Offset(cx - 12f * s, h * 0.88f - rise), accent, lw)
                limb(Offset(cx + 12f * s, h * 0.8f - rise), Offset(cx + 12f * s, h * 0.88f - rise), accent, lw)
            }
            AnimKind.WRIST -> {
                val bend = (p - 0.5f) * 1.2f
                drawCircle(color, 14f * s, Offset(cx - 40f * s, h * 0.34f))
                limb(Offset(cx - 40f * s, h * 0.4f), Offset(cx - 40f * s, h * 0.66f), color, lw)
                limb(Offset(cx - 40f * s, h * 0.66f), Offset(cx - 52f * s, h * 0.88f), color, lw)
                limb(Offset(cx - 40f * s, h * 0.66f), Offset(cx - 28f * s, h * 0.88f), color, lw)
                val elbow = Offset(cx - 40f * s + 34f * s, h * 0.5f)
                limb(Offset(cx - 40f * s, h * 0.46f), elbow, color, lw)
                val hand = Offset(elbow.x + cos(bend) * 34f * s, elbow.y + sin(bend) * 34f * s)
                limb(elbow, hand, accent, lw)
                drawCircle(accent, 6f * s, hand)
            }
            AnimKind.HIP -> {
                val open = p * 0.9f
                drawCircle(color, 14f * s, Offset(cx, h * 0.32f))
                limb(Offset(cx, h * 0.38f), Offset(cx, h * 0.6f), color, lw)
                limb(Offset(cx, h * 0.44f), Offset(cx - 28f * s, h * 0.52f), color, lw)
                limb(Offset(cx, h * 0.44f), Offset(cx + 28f * s, h * 0.52f), color, lw)
                limb(Offset(cx, h * 0.6f), Offset(cx - 18f * s, h * 0.88f), color, lw)
                val knee = Offset(cx + sin(open) * 34f * s, h * 0.72f)
                limb(Offset(cx, h * 0.6f), knee, accent, lw)
                limb(knee, Offset(knee.x + 8f * s, h * 0.88f), accent, lw)
            }
        }
    }
}
