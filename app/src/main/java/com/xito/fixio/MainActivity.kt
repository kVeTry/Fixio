package com.xito.fixio

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val perm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
        if (Build.VERSION.SDK_INT >= 33) perm.launch(Manifest.permission.POST_NOTIFICATIONS)
        Reminders.schedule(this)
        setContent {
            var themeMode by remember { mutableStateOf(Store.theme(this)) }
            FixioTheme(themeMode) {
                val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    AnimatedBackground(dark)
                    App(onThemeChange = { themeMode = it })
                }
            }
        }
    }
}

@Composable
fun App(onThemeChange: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var onboarded by remember { mutableStateOf(Store.onboarded(ctx)) }
    if (!onboarded) { Onboarding { onboarded = true }; return }

    var tab by remember { mutableStateOf(0) }
    var refresh by remember { mutableStateOf(0) }
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                val items = listOf(
                    Triple("Inicio", Icons.Filled.Home, 0),
                    Triple("Registro", Icons.Filled.TouchApp, 1),
                    Triple("Ejercicios", Icons.Filled.FitnessCenter, 2),
                    Triple("Progreso", Icons.Filled.ShowChart, 3),
                    Triple("Más", Icons.Filled.GridView, 4)
                )
                items.forEach { (label, icon, idx) ->
                    NavigationBarItem(
                        selected = tab == idx,
                        onClick = { tab = idx },
                        icon = { Icon(icon, label) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { pad ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(320)) { it / 6 * dir } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(320)) { -it / 6 * dir } + fadeOut(tween(200)))
            },
            label = "tab",
            modifier = Modifier.padding(pad)
        ) { current ->
            when (current) {
                0 -> HomeScreen(refresh) { tab = 1 }
                1 -> RegisterScreen { refresh++; tab = 0 }
                2 -> ExercisesScreen(refresh)
                3 -> ProgressScreen(refresh)
                4 -> MoreScreen(onThemeChange, refresh)
            }
        }
    }
}

@Composable
fun Onboarding(done: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("m") }
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WellnessArt(Modifier.size(160.dp), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        Text("Bienvenido a Fixio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tu asistente para registrar dolencias, recibir consejos y ejercicios personalizados, y ver tu evolución día a día.",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Tu nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text("Selecciona tu cuerpo", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(gender == "m", { gender = "m" }, label = { Text("♂ Masculino") })
            FilterChip(gender == "f", { gender = "f" }, label = { Text("♀ Femenino") })
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                Store.setName(ctx, name.ifBlank { "Usuario" })
                Store.setGender(ctx, gender)
                Store.setOnboarded(ctx)
                done()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Empezar", fontSize = 16.sp) }
        Spacer(Modifier.height(16.dp))
        Text(
            "⚠️ Fixio no reemplaza el consejo médico profesional. Es una herramienta de apoyo y bienestar.",
            fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatCard(modifier: Modifier, emoji: String, value: String, label: String) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 26.sp)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HomeScreen(refresh: Int, goRegister: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val entries = remember(refresh) { Store.entries(ctx) }
    val today = entries.find { it.date == Store.today() }
    val streak = remember(refresh) { Store.streak(ctx) }
    val avg7 = entries.takeLast(7).flatMap { it.points }.map { it.score }.let { if (it.isEmpty()) 0.0 else it.average() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Hola, ${Store.name(ctx)} 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            SimpleDateFormat("EEEE d 'de' MMMM", Locale("es")).format(Date()).replaceFirstChar { it.uppercase() },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SlideFadeIn(Modifier.weight(1f), 0) { StatCard(Modifier.fillMaxWidth(), "🔥", "${animatedInt(streak)}", "Días de racha") }
            SlideFadeIn(Modifier.weight(1f), 1) { StatCard(Modifier.fillMaxWidth(), "📊", String.format(Locale.US, "%.1f", animatedFloat1(avg7.toFloat())), "Dolor medio 7d") }
            SlideFadeIn(Modifier.weight(1f), 2) { StatCard(Modifier.fillMaxWidth(), "📝", "${animatedInt(entries.size)}", "Registros") }
        }
        Spacer(Modifier.height(20.dp))
        if (today == null) {
            Card(
                Modifier.fillMaxWidth().clickable { goRegister() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(Modifier.fillMaxWidth().background(animatedHeroBrush())) {
                    Column(Modifier.padding(24.dp)) {
                        Text("¿Cómo te encuentras hoy?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Toca aquí para marcar tus zonas de dolor en el cuerpo interactivo.", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("✅ Registro de hoy completado", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    if (today.points.isEmpty()) Text("Sin dolores registrados. ¡Genial!")
                    else today.points.sortedByDescending { it.score }.forEach { p ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(scoreColor(p.score)))
                            Spacer(Modifier.width(8.dp))
                            Text("${Zones.byId(p.zoneId).name} · ${p.score}/10")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = goRegister) { Text("Editar registro") }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Consejo del día 💡", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        val tips = listOf(
            "Levántate y muévete 2-3 minutos por cada hora sentado.",
            "El calor relaja la tensión muscular; el frío calma inflamaciones recientes.",
            "Dormir de lado con una almohada entre las rodillas alivia la zona lumbar.",
            "Una buena hidratación mejora la recuperación muscular.",
            "Estirar 10 minutos al despertar reduce la rigidez matutina.",
            "Ajusta la pantalla a la altura de los ojos para cuidar el cuello.",
            "Caminar 20-30 minutos diarios es uno de los mejores analgésicos naturales."
        )
        val dayIdx = (SimpleDateFormat("D", Locale.US).format(Date()).toInt()) % tips.size
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Text(tips[dayIdx], Modifier.padding(18.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "⚠️ Fixio no reemplaza el consejo médico. Ante dolor intenso o persistente, consulta a un profesional.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun RegisterScreen(onSaved: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val existing = remember { Store.entries(ctx).find { it.date == Store.today() } }
    var back by remember { mutableStateOf(false) }
    val scores = remember {
        mutableStateMapOf<String, Int>().apply { existing?.points?.forEach { put(it.zoneId, it.score) } }
    }
    val notes = remember {
        mutableStateMapOf<String, String>().apply { existing?.points?.forEach { put(it.zoneId, it.note) } }
    }
    var editingZone by remember { mutableStateOf<String?>(null) }
    var mood by remember { mutableStateOf(existing?.mood ?: 3) }
    var sleep by remember { mutableStateOf(existing?.sleep ?: 3) }
    var activity by remember { mutableStateOf(existing?.activity ?: 2) }
    var aiResult by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val female = Store.gender(ctx) == "f"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Registro diario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Toca las zonas donde sientas dolor", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(!back, { back = false }, label = { Text("Frente") })
            FilterChip(back, { back = true }, label = { Text("Espalda") })
            Spacer(Modifier.weight(1f))
            AssistChip(onClick = {}, label = { Text(if (female) "♀" else "♂") })
        }
        Spacer(Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            BodyMap(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(bodyAspect())
                    .padding(8.dp),
                back = back, female = female, scores = scores,
                onZoneTap = { editingZone = it }
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(2 to "Leve", 5 to "Moderado", 8 to "Intenso").forEach { (s, l) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(scoreColor(s)))
                    Spacer(Modifier.width(4.dp)); Text(l, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (scores.isNotEmpty()) {
            Text("Zonas marcadas", fontWeight = FontWeight.Bold)
            scores.forEach { (z, s) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { editingZone = z }, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(scoreColor(s)))
                        Spacer(Modifier.width(10.dp))
                        Text("${Zones.byId(z).name} · $s/10", Modifier.weight(1f))
                        IconButton({ scores.remove(z); notes.remove(z) }) { Icon(Icons.Filled.Close, "Quitar") }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Tu día", fontWeight = FontWeight.Bold)
        SliderRow("😊 Ánimo", mood, 5) { mood = it }
        SliderRow("😴 Sueño", sleep, 5) { sleep = it }
        SliderRow("🏃 Actividad", activity, 5) { activity = it }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val entry = DayEntry(
                    Store.today(),
                    scores.map { (z, s) -> PainPoint(z, s, notes[z] ?: "") },
                    mood, sleep, activity
                )
                Store.saveEntry(ctx, entry)
                ExtraStore.markRegisteredLate(ctx)
                loading = true
                scope.launch {
                    aiResult = Ai.diagnose(ctx, entry, Store.entries(ctx))
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)
        ) { Text("Guardar y analizar 🧠") }
        Spacer(Modifier.height(12.dp))
        if (loading) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        aiResult?.let { r ->
            Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("🧠 Análisis", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(r)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onSaved, modifier = Modifier.fillMaxWidth()) { Text("Hecho ✓") }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    editingZone?.let { z ->
        var temp by remember(z) { mutableStateOf(scores[z] ?: 5) }
        var noteTemp by remember(z) { mutableStateOf(notes[z] ?: "") }
        AlertDialog(
            onDismissRequest = { editingZone = null },
            title = { Text(Zones.byId(z).name) },
            text = {
                Column {
                    Text("Nivel de dolor: $temp/10", fontWeight = FontWeight.Bold, color = scoreColor(temp))
                    Slider(temp.toFloat(), { temp = it.toInt().coerceIn(1, 10) }, valueRange = 1f..10f, steps = 8)
                    OutlinedTextField(noteTemp, { noteTemp = it }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton({ scores[z] = temp; notes[z] = noteTemp; editingZone = null }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton({ scores.remove(z); notes.remove(z); editingZone = null }) { Text("Quitar zona") }
            }
        )
    }
}

@Composable
fun SliderRow(label: String, value: Int, max: Int, onChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text("$label: $value/$max", fontSize = 13.sp)
        Slider(value.toFloat(), { onChange(it.toInt().coerceIn(1, max)) }, valueRange = 1f..max.toFloat(), steps = max - 2)
    }
}

@Composable
fun ExercisesScreen(refresh: Int) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val today = remember(refresh) { Store.entries(ctx).find { it.date == Store.today() } }
    val zones = today?.points?.sortedByDescending { it.score }?.map { it.zoneId }
        ?: listOf("lumbar", "cuello")
    var expanded by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<Exercise?>(null) }
    var breathing by remember { mutableStateOf(false) }
    var doneTick by remember { mutableStateOf(0) }
    val done = remember(doneTick) { ExtraStore.doneToday(ctx) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
        item {
            Text("Ejercicios personalizados", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (today?.points?.isNotEmpty() == true) "Basados en tu registro de hoy · ${done.size} completados hoy"
                else "Registra tu dolor hoy para personalizarlos. Mientras, algunos generales:",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.fillMaxWidth().clickable { breathing = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🫁", fontSize = 28.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Respiración guiada 4-7-8", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Relaja tensión muscular y calma el dolor", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        zones.forEach { zid ->
            item {
                Text("📍 ${Zones.byId(zid).name}", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
            items(ExerciseDb.forZone(zid)) { ex ->
                val key = "$zid-${ex.name}"
                val isDone = done.contains(key)
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).animateContentSize()
                        .clickable { expanded = if (expanded == key) null else key },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDone) { Text("✅", fontSize = 16.sp); Spacer(Modifier.width(6.dp)) }
                            Text(ex.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(ex.duration, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        if (expanded == key) {
                            Spacer(Modifier.height(10.dp))
                            ex.steps.forEachIndexed { i, s ->
                                Row(Modifier.padding(vertical = 3.dp)) {
                                    Text("${i + 1}.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(s, fontSize = 14.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("⚠️ ${ex.caution}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button({ player = ex }, Modifier.weight(1f)) { Text("▶ Guía") }
                                OutlinedButton({ ExtraStore.toggleDone(ctx, key); doneTick++ }, Modifier.weight(1f)) {
                                    Text(if (isDone) "Desmarcar" else "Marcar hecho")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    player?.let { ex ->
        ExercisePlayer(ex, onClose = { player = null }, onComplete = {
            ExtraStore.toggleDone(ctx, "${zones.first()}-${ex.name}"); doneTick++
        })
    }
    if (breathing) BreathingScreen { breathing = false }
}

@Composable
fun ProgressScreen(refresh: Int) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val entries = remember(refresh) { Store.entries(ctx) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Tu progreso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (entries.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyArt(Modifier.size(120.dp), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Aún no hay registros. Empieza hoy y verás aquí tu evolución 📈", textAlign = TextAlign.Center)
                }
            }
        } else {
            val last = entries.takeLast(14)
            Text("Dolor medio diario (últimos 14 días)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                LineChart(
                    Modifier.fillMaxWidth().height(180.dp).padding(16.dp),
                    last.map { d -> if (d.points.isEmpty()) 0f else d.points.map { it.score }.average().toFloat() },
                    10f, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Nº de zonas con dolor por día", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                BarChart(
                    Modifier.fillMaxWidth().height(140.dp).padding(16.dp),
                    last.map { it.points.size.toFloat() },
                    (last.maxOf { it.points.size }.coerceAtLeast(1)).toFloat(),
                    MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Zonas más frecuentes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            val freq = entries.flatMap { it.points }.groupBy { it.zoneId }
                .mapValues { (_, v) -> v.size to v.map { it.score }.average() }
                .entries.sortedByDescending { it.value.first }.take(6)
            freq.forEach { (z, p) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(scoreColor(p.second.toInt())))
                        Spacer(Modifier.width(10.dp))
                        Text(Zones.byId(z).name, Modifier.weight(1f))
                        Text("${p.first} días · media ${String.format(Locale.US, "%.1f", p.second)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Mapa de calor corporal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Media histórica de dolor por zona", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val heat = entries.flatMap { it.points }.groupBy { it.zoneId }
                .mapValues { (_, v) -> v.map { it.score }.average().toInt().coerceIn(1, 10) }
            val female = Store.gender(ctx) == "f"
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Frente", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BodyMap(Modifier.width(130.dp).aspectRatio(bodyAspect()), false, female, heat) {}
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Espalda", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BodyMap(Modifier.width(130.dp).aspectRatio(bodyAspect()), true, female, heat) {}
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val trend = run {
                val prev = entries.dropLast(7).takeLast(7).flatMap { it.points }.map { it.score }
                val now = entries.takeLast(7).flatMap { it.points }.map { it.score }
                if (prev.isEmpty() || now.isEmpty()) null else now.average() - prev.average()
            }
            trend?.let {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        if (it < -0.3) "📉 ¡Buenas noticias! Tu dolor medio ha bajado ${String.format(Locale.US, "%.1f", -it)} puntos respecto a la semana anterior."
                        else if (it > 0.3) "📈 Tu dolor medio ha subido ${String.format(Locale.US, "%.1f", it)} puntos esta semana. Revisa posturas y descanso."
                        else "➡️ Tu dolor se mantiene estable respecto a la semana anterior.",
                        Modifier.padding(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun MoreScreen(onThemeChange: (String) -> Unit, refresh: Int) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val unlocked = remember(refresh) { Achievements.unlocked(ctx) }
    var medTick by remember { mutableStateOf(0) }
    val meds = remember(medTick) { ExtraStore.meds(ctx) }
    var showMedDialog by remember { mutableStateOf(false) }
    var reportCopied by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Más", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // LOGROS
        Text("🏅 Logros (${unlocked.size}/${Achievements.all.size})", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Achievements.all.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { a ->
                    val on = unlocked.contains(a.id)
                    Card(
                        Modifier.weight(1f).padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if (on) a.emoji else "🔒", fontSize = 22.sp)
                            Text(a.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(a.desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(20.dp))

        // MEDICACIÓN / TRATAMIENTOS
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💊 Tratamientos", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton({ showMedDialog = true }) { Text("+ Añadir") }
        }
        if (meds.isEmpty()) Text("Sin tratamientos. Añade medicación o pautas para llevar el control diario.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        meds.forEach { m ->
            val taken = ExtraStore.medTakenToday(ctx, m.id)
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(taken, { ExtraStore.toggleMedTaken(ctx, m.id); medTick++ })
                    Column(Modifier.weight(1f)) {
                        Text("${m.name} ${m.dose}", fontWeight = FontWeight.SemiBold)
                        Text(m.schedule, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton({ ExtraStore.deleteMed(ctx, m.id); medTick++ }) { Icon(Icons.Filled.Delete, "Borrar") }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // INFORME MÉDICO
        Text("📄 Informe para el médico", fontWeight = FontWeight.SemiBold)
        Text("Resumen de tus registros para compartir en consulta.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Button({
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("informe", Report.forDoctor(ctx)))
            reportCopied = true
        }, shape = RoundedCornerShape(14.dp)) { Text("📋 Copiar informe") }
        if (reportCopied) Text("Informe copiado ✓", fontSize = 12.sp, color = Teal)
        Spacer(Modifier.height(24.dp))

        SettingsSection(onThemeChange)
    }

    if (showMedDialog) {
        var mn by remember { mutableStateOf("") }
        var md by remember { mutableStateOf("") }
        var ms by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMedDialog = false },
            title = { Text("Nuevo tratamiento") },
            text = {
                Column {
                    OutlinedTextField(mn, { mn = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(md, { md = it }, label = { Text("Dosis (ej: 600mg)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(ms, { ms = it }, label = { Text("Pauta (ej: cada 8h)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton({
                    if (mn.isNotBlank()) {
                        ExtraStore.saveMed(ctx, Medication(UUID.randomUUID().toString(), mn, md, ms, true))
                        medTick++
                    }
                    showMedDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton({ showMedDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun SettingsSection(onThemeChange: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var theme by remember { mutableStateOf(Store.theme(ctx)) }
    var gender by remember { mutableStateOf(Store.gender(ctx)) }
    var hour by remember { mutableStateOf(Store.reminderHour(ctx)) }
    var minute by remember { mutableStateOf(Store.reminderMin(ctx)) }
    var key by remember { mutableStateOf(Store.apiKey(ctx)) }
    var name by remember { mutableStateOf(Store.name(ctx)) }
    var copied by remember { mutableStateOf(false) }

    Column {
        Text("⚙️ Ajustes", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        Text("Tema", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Auto", "light" to "☀️ Claro", "dark" to "🌙 Oscuro", "amoled" to "⬛ AMOLED").forEach { (v, l) ->
                FilterChip(theme == v, { theme = v; Store.setTheme(ctx, v); onThemeChange(v) }, label = { Text(l, fontSize = 12.sp) })
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Perfil", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(name, { name = it; Store.setName(ctx, it) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(gender == "m", { gender = "m"; Store.setGender(ctx, "m") }, label = { Text("♂ Masculino") })
            FilterChip(gender == "f", { gender = "f"; Store.setGender(ctx, "f") }, label = { Text("♀ Femenino") })
        }
        Spacer(Modifier.height(20.dp))

        Text("Recordatorio diario", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                hour.toString(), { it.toIntOrNull()?.let { h -> if (h in 0..23) { hour = h; Store.setReminder(ctx, h, minute); Reminders.schedule(ctx) } } },
                label = { Text("Hora") }, singleLine = true, modifier = Modifier.width(100.dp)
            )
            Text(":")
            OutlinedTextField(
                minute.toString(), { it.toIntOrNull()?.let { m -> if (m in 0..59) { minute = m; Store.setReminder(ctx, hour, m); Reminders.schedule(ctx) } } },
                label = { Text("Min") }, singleLine = true, modifier = Modifier.width(100.dp)
            )
        }
        Spacer(Modifier.height(20.dp))

        Text("IA avanzada (opcional)", fontWeight = FontWeight.SemiBold)
        Text("Con una clave API de Anthropic el análisis diario lo hace Claude. Sin clave, Fixio usa su motor local de consejos.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(key, { key = it; Store.setApiKey(ctx, it) }, label = { Text("Clave API (sk-ant-...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))

        Text("Datos", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("fixio", Store.exportCsv(ctx)))
            copied = true
        }, shape = RoundedCornerShape(14.dp)) { Text("📋 Copiar historial (CSV)") }
        if (copied) Text("Copiado al portapapeles ✓", fontSize = 12.sp, color = Teal)
        Spacer(Modifier.height(24.dp))
        Text("Fixio v1.2 · Xito Development\n⚠️ Esta app no reemplaza el consejo médico profesional.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
    }
}
