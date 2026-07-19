package com.xito.fixio

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ExercisePlayer(exercise: Exercise, onClose: () -> Unit, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(30) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running, step) {
        while (running && seconds > 0) { delay(1000); seconds-- }
        if (seconds == 0) running = false
    }

    Dialog(onDismiss = onClose) {
        Column(Modifier.padding(24.dp)) {
            Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Paso ${step + 1} de ${exercise.steps.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (step + 1f) / exercise.steps.size },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(exercise.steps[step], Modifier.padding(20.dp).fillMaxWidth(), fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
            Text("$seconds s", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { s ->
                    AssistChip(onClick = { seconds = s; running = false }, label = { Text("${s}s", fontSize = 11.sp) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton({ running = !running }, Modifier.weight(1f)) { Text(if (running) "⏸ Pausa" else "▶ Iniciar") }
                Button({ seconds = 30; running = false }, Modifier.weight(1f)) { Text("↻ Reiniciar") }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) OutlinedButton({ step--; seconds = 30; running = false }, Modifier.weight(1f)) { Text("Anterior") }
                if (step < exercise.steps.size - 1) Button({ step++; seconds = 30; running = false }, Modifier.weight(1f)) { Text("Siguiente") }
                else Button({ onComplete(); onClose() }, Modifier.weight(1f)) { Text("Completar ✓") }
            }
            Spacer(Modifier.height(8.dp))
            Text("⚠️ ${exercise.caution}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            TextButton(onClose, Modifier.align(Alignment.CenterHorizontally)) { Text("Cerrar") }
        }
    }
}

@Composable
fun BreathingScreen(onClose: () -> Unit) {
    // ciclo 4-7-8: inspira 4, mantén 7, exhala 8
    val phases = listOf("Inspira" to 4, "Mantén" to 7, "Exhala" to 8)
    var phase by remember { mutableStateOf(0) }
    var count by remember { mutableStateOf(phases[0].second) }
    var cycles by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (count > 1) count-- else {
                phase = (phase + 1) % phases.size
                if (phase == 0) cycles++
                count = phases[phase].second
            }
        }
    }
    val target = when (phase) { 0 -> 1f; 1 -> 1f; else -> 0.45f }
    val scale by animateFloatAsState(target, tween(if (phase == 0) 4000 else if (phase == 2) 8000 else 300), label = "b")

    Dialog(onDismiss = onClose) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Respiración 4-7-8", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Relaja tensión y dolor", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                val col = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f * scale
                    drawCircle(col.copy(alpha = 0.15f), size.minDimension / 2f, Offset(size.width / 2, size.height / 2))
                    drawCircle(col.copy(alpha = 0.55f), r, Offset(size.width / 2, size.height / 2))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(phases[phase].first, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("$count", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Ciclos completados: $cycles", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Button(onClose, Modifier.fillMaxWidth()) { Text("Terminar") }
        }
    }
}

@Composable
fun Dialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            content()
        }
    }
}
