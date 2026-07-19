package com.xito.fixio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Zone(val id: String, val name: String, val back: Boolean)

object Zones {
    val all = listOf(
        Zone("cabeza", "Cabeza", false),
        Zone("cuello", "Cuello", false),
        Zone("hombro_izq", "Hombro izquierdo", false),
        Zone("hombro_der", "Hombro derecho", false),
        Zone("pecho", "Pecho", false),
        Zone("abdomen", "Abdomen", false),
        Zone("brazo_izq", "Brazo izquierdo", false),
        Zone("brazo_der", "Brazo derecho", false),
        Zone("codo_izq", "Codo izquierdo", false),
        Zone("codo_der", "Codo derecho", false),
        Zone("muneca_izq", "Muñeca izquierda", false),
        Zone("muneca_der", "Muñeca derecha", false),
        Zone("cadera", "Cadera", false),
        Zone("muslo_izq", "Muslo izquierdo", false),
        Zone("muslo_der", "Muslo derecho", false),
        Zone("rodilla_izq", "Rodilla izquierda", false),
        Zone("rodilla_der", "Rodilla derecha", false),
        Zone("tobillo_izq", "Tobillo izquierdo", false),
        Zone("tobillo_der", "Tobillo derecho", false),
        Zone("pie_izq", "Pie izquierdo", false),
        Zone("pie_der", "Pie derecho", false),
        Zone("nuca", "Nuca", true),
        Zone("espalda_alta", "Espalda alta", true),
        Zone("espalda_media", "Espalda media", true),
        Zone("lumbar", "Zona lumbar", true),
        Zone("gluteo_izq", "Glúteo izquierdo", true),
        Zone("gluteo_der", "Glúteo derecho", true),
        Zone("isquio_izq", "Isquio izquierdo", true),
        Zone("isquio_der", "Isquio derecho", true),
        Zone("gemelo_izq", "Gemelo izquierdo", true),
        Zone("gemelo_der", "Gemelo derecho", true),
        Zone("talon_izq", "Talón izquierdo", true),
        Zone("talon_der", "Talón derecho", true)
    )
    fun byId(id: String) = all.first { it.id == id }
}

data class PainPoint(val zoneId: String, val score: Int, val note: String)
data class DayEntry(val date: String, val points: List<PainPoint>, val mood: Int, val sleep: Int, val activity: Int)

data class Exercise(
    val name: String,
    val duration: String,
    val steps: List<String>,
    val caution: String
)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("fixio", Context.MODE_PRIVATE)

    fun gender(c: Context) = prefs(c).getString("gender", "m")!!
    fun setGender(c: Context, g: String) = prefs(c).edit().putString("gender", g).apply()
    fun theme(c: Context) = prefs(c).getString("theme", "system")!!
    fun setTheme(c: Context, t: String) = prefs(c).edit().putString("theme", t).apply()
    fun apiKey(c: Context) = prefs(c).getString("apikey", "")!!
    fun setApiKey(c: Context, k: String) = prefs(c).edit().putString("apikey", k).apply()
    fun reminderHour(c: Context) = prefs(c).getInt("remHour", 20)
    fun reminderMin(c: Context) = prefs(c).getInt("remMin", 0)
    fun setReminder(c: Context, h: Int, m: Int) = prefs(c).edit().putInt("remHour", h).putInt("remMin", m).apply()
    fun name(c: Context) = prefs(c).getString("name", "")!!
    fun setName(c: Context, n: String) = prefs(c).edit().putString("name", n).apply()
    fun onboarded(c: Context) = prefs(c).getBoolean("onboarded", false)
    fun setOnboarded(c: Context) = prefs(c).edit().putBoolean("onboarded", true).apply()

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun entries(c: Context): List<DayEntry> {
        val raw = prefs(c).getString("entries", "[]")!!
        val arr = JSONArray(raw)
        val out = ArrayList<DayEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val pts = ArrayList<PainPoint>()
            val pArr = o.getJSONArray("points")
            for (j in 0 until pArr.length()) {
                val p = pArr.getJSONObject(j)
                pts.add(PainPoint(p.getString("z"), p.getInt("s"), p.optString("n")))
            }
            out.add(DayEntry(o.getString("date"), pts, o.optInt("mood", 3), o.optInt("sleep", 3), o.optInt("activity", 2)))
        }
        return out.sortedBy { it.date }
    }

    fun saveEntry(c: Context, e: DayEntry) {
        val list = entries(c).filter { it.date != e.date } + e
        val arr = JSONArray()
        list.sortedBy { it.date }.forEach { d ->
            val o = JSONObject()
            o.put("date", d.date)
            o.put("mood", d.mood); o.put("sleep", d.sleep); o.put("activity", d.activity)
            val pArr = JSONArray()
            d.points.forEach { p ->
                pArr.put(JSONObject().put("z", p.zoneId).put("s", p.score).put("n", p.note))
            }
            o.put("points", pArr)
            arr.put(o)
        }
        prefs(c).edit().putString("entries", arr.toString()).apply()
    }

    fun streak(c: Context): Int {
        val dates = entries(c).map { it.date }.toSet()
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var s = 0
        while (dates.contains(fmt.format(cal.time))) { s++; cal.add(Calendar.DAY_OF_YEAR, -1) }
        return s
    }

    fun exportCsv(c: Context): String {
        val sb = StringBuilder("fecha,zona,dolor,nota,animo,sueno,actividad\n")
        entries(c).forEach { d ->
            if (d.points.isEmpty()) sb.append("${d.date},,,,${d.mood},${d.sleep},${d.activity}\n")
            d.points.forEach { p ->
                sb.append("${d.date},${Zones.byId(p.zoneId).name},${p.score},\"${p.note}\",${d.mood},${d.sleep},${d.activity}\n")
            }
        }
        return sb.toString()
    }
}

object ExerciseDb {
    private fun e(n: String, d: String, c: String, vararg s: String) = Exercise(n, d, s.toList(), c)

    fun forZone(zoneId: String): List<Exercise> = when {
        zoneId in listOf("cuello", "nuca", "cabeza") -> listOf(
            e("Inclinaciones laterales de cuello", "2 min · 3x10 por lado", "Para si notas mareo u hormigueo.",
                "Siéntate con la espalda recta y los hombros relajados.",
                "Inclina la cabeza lentamente hacia el hombro derecho sin levantarlo.",
                "Mantén 5 segundos sintiendo el estiramiento en el lado izquierdo.",
                "Vuelve al centro y repite hacia el otro lado.",
                "Respira profundo durante todo el movimiento."),
            e("Retracción cervical (chin tuck)", "2 min · 3x10", "No fuerces si hay dolor agudo irradiado al brazo.",
                "Sentado o de pie, mira al frente.",
                "Lleva la barbilla hacia atrás como haciendo 'papada', sin inclinar la cabeza.",
                "Mantén 5 segundos y relaja.",
                "Repite 10 veces, 3 series al día."),
            e("Rotaciones suaves", "1 min · 2x8 por lado", "Movimientos lentos, nunca bruscos.",
                "Gira la cabeza lentamente a la derecha hasta donde llegues sin dolor.",
                "Mantén 3 segundos y vuelve al centro.",
                "Repite al lado izquierdo.")
        )
        zoneId in listOf("hombro_izq", "hombro_der") -> listOf(
            e("Péndulo de Codman", "3 min", "Ideal en fases dolorosas; el brazo cuelga relajado.",
                "Apóyate con el brazo sano en una mesa e inclina el tronco.",
                "Deja colgar el brazo afectado totalmente relajado.",
                "Haz pequeños círculos con el cuerpo, no con el brazo, 30s por dirección.",
                "Aumenta el círculo gradualmente si no hay dolor."),
            e("Rotación externa con banda", "3x12", "Codo pegado al cuerpo en todo momento.",
                "Ata una banda elástica a la altura del codo.",
                "Con el codo a 90° pegado al costado, gira el antebrazo hacia fuera.",
                "Vuelve lento (2-3 segundos) a la posición inicial."),
            e("Deslizamiento en pared", "3x10", "Sube solo hasta donde no haya dolor.",
                "De cara a la pared, apoya las manos a la altura del pecho.",
                "Desliza las manos hacia arriba estirando los brazos.",
                "Baja controlando el movimiento.")
        )
        zoneId in listOf("espalda_alta", "espalda_media") -> listOf(
            e("Gato-camello", "2 min · 10 reps", "Movimiento fluido, sin rebotes.",
                "A cuatro patas, manos bajo hombros y rodillas bajo caderas.",
                "Arquea la espalda hacia arriba metiendo la barbilla (gato).",
                "Luego hunde la espalda mirando al frente (camello).",
                "Alterna lentamente coordinando con la respiración."),
            e("Apertura torácica en pared", "3x8 por lado", "Evita si hay dolor punzante.",
                "Túmbate de lado con rodillas flexionadas y brazos extendidos al frente.",
                "Abre el brazo superior hacia el otro lado siguiendo la mano con la mirada.",
                "Mantén 3 segundos y vuelve."),
            e("Estiramiento del trapecio", "3x20s por lado", "Tira suave, sin dolor.",
                "Sentado, sujeta el borde de la silla con una mano.",
                "Inclina la cabeza al lado contrario y ligeramente hacia delante.",
                "Mantén 20 segundos respirando.")
        )
        zoneId == "lumbar" -> listOf(
            e("Rodillas al pecho", "3x30s", "Si el dolor baja por la pierna, consulta a un profesional.",
                "Túmbate boca arriba en una superficie firme.",
                "Lleva ambas rodillas al pecho abrazándolas suavemente.",
                "Mantén 30 segundos respirando profundo.",
                "Suelta lentamente y repite."),
            e("Puente de glúteos", "3x12", "No arquees en exceso la zona lumbar.",
                "Boca arriba, rodillas flexionadas y pies apoyados al ancho de caderas.",
                "Aprieta glúteos y eleva la cadera hasta alinear rodillas-cadera-hombros.",
                "Mantén 3 segundos arriba y baja vértebra a vértebra."),
            e("Bird-dog", "3x8 por lado", "Mantén el abdomen activado, sin balanceo.",
                "A cuatro patas, extiende el brazo derecho y la pierna izquierda a la vez.",
                "Mantén 5 segundos con la cadera estable.",
                "Cambia de lado."),
            e("Postura del niño", "3x30s", "Estiramiento suave de descarga.",
                "De rodillas, siéntate sobre los talones.",
                "Estira los brazos al frente apoyando la frente en el suelo.",
                "Respira llevando el aire a la espalda.")
        )
        zoneId in listOf("rodilla_izq", "rodilla_der") -> listOf(
            e("Extensión de cuádriceps sentado", "3x12", "Sin bloquear la rodilla bruscamente.",
                "Sentado en una silla con la espalda recta.",
                "Extiende la pierna hasta estirarla, contrayendo el muslo.",
                "Mantén 5 segundos y baja lento."),
            e("Sentadilla parcial asistida", "3x10", "Baja solo hasta donde no haya dolor.",
                "Apóyate en una mesa o encimera.",
                "Flexiona las rodillas hasta unos 45° manteniendo talones en el suelo.",
                "Sube apretando glúteos y muslos."),
            e("Estiramiento de isquios", "3x20s por pierna", "Espalda recta, sin rebotar.",
                "Apoya el talón en un escalón bajo con la pierna estirada.",
                "Inclina el tronco desde la cadera hasta notar tensión detrás del muslo.",
                "Mantén 20 segundos.")
        )
        zoneId in listOf("gemelo_izq", "gemelo_der", "tobillo_izq", "tobillo_der", "talon_izq", "talon_der", "pie_izq", "pie_der") -> listOf(
            e("Estiramiento de gemelo en pared", "3x30s por pierna", "Talón siempre apoyado.",
                "Manos en la pared, una pierna atrás estirada con el talón en el suelo.",
                "Adelanta la cadera hasta notar el estiramiento en el gemelo.",
                "Mantén 30 segundos."),
            e("Elevación de talones", "3x15", "Sube y baja controlado.",
                "De pie, apóyate ligeramente en una pared.",
                "Ponte de puntillas elevando los talones al máximo.",
                "Baja en 3 segundos."),
            e("Rodar pelota bajo el pie", "2 min por pie", "Presión agradable, no dolorosa.",
                "Sentado, coloca una pelota pequeña bajo la planta del pie.",
                "Ruédala lentamente del talón a los dedos masajeando la fascia."),
            e("Alfabeto con el tobillo", "1-2 min", "Movimiento amplio y lento.",
                "Sentado, eleva ligeramente el pie.",
                "Dibuja en el aire las letras del abecedario con la punta del pie.")
        )
        zoneId in listOf("muneca_izq", "muneca_der", "codo_izq", "codo_der", "brazo_izq", "brazo_der") -> listOf(
            e("Estiramiento de flexores de muñeca", "3x20s", "Tira suave.",
                "Extiende el brazo con la palma hacia arriba.",
                "Con la otra mano lleva los dedos hacia abajo y atrás.",
                "Mantén 20 segundos y cambia."),
            e("Estiramiento de extensores", "3x20s", "Sin dolor punzante en el codo.",
                "Extiende el brazo con la palma hacia abajo.",
                "Empuja el dorso de la mano hacia ti con la otra mano.",
                "Mantén 20 segundos."),
            e("Rotaciones de muñeca", "1 min", "Lento y amplio.",
                "Junta las manos entrelazando los dedos.",
                "Dibuja ochos con las muñecas en ambas direcciones."),
            e("Apretar pelota blanda", "3x10", "Fuerza moderada.",
                "Sujeta una pelota blanda o antiestrés.",
                "Apriétala 5 segundos y suelta.")
        )
        zoneId in listOf("cadera", "gluteo_izq", "gluteo_der", "muslo_izq", "muslo_der", "isquio_izq", "isquio_der") -> listOf(
            e("Estiramiento del piramidal (figura 4)", "3x30s por lado", "Estira sin dolor agudo.",
                "Boca arriba, cruza un tobillo sobre la rodilla contraria.",
                "Abraza el muslo de la pierna de apoyo y tira suavemente hacia el pecho.",
                "Mantén 30 segundos."),
            e("Zancada de flexor de cadera", "3x20s por lado", "Pelvis neutra, sin arquear la lumbar.",
                "Rodilla derecha al suelo, pie izquierdo delante.",
                "Adelanta la cadera hasta notar el estiramiento en la ingle derecha.",
                "Mantén 20 segundos y cambia."),
            e("Concha (clamshell)", "3x12 por lado", "Cadera estable, no ruedes hacia atrás.",
                "Túmbate de lado con rodillas flexionadas.",
                "Abre la rodilla superior manteniendo los pies juntos.",
                "Baja lento."),
            e("Puente de glúteos", "3x12", "Aprieta glúteos arriba.",
                "Boca arriba con rodillas flexionadas.",
                "Eleva la cadera y mantén 3 segundos.",
                "Baja controladamente.")
        )
        else -> listOf(
            e("Respiración diafragmática", "5 min", "Ejercicio seguro para casi todo el mundo.",
                "Túmbate boca arriba con una mano en el pecho y otra en el abdomen.",
                "Inspira por la nariz 4 segundos hinchando el abdomen.",
                "Exhala por la boca 6 segundos.",
                "Repite durante 5 minutos."),
            e("Caminata suave", "15-20 min", "Ritmo cómodo, superficie llana.",
                "Camina a paso cómodo manteniendo buena postura.",
                "Hombros relajados y mirada al frente.",
                "Aumenta 5 minutos cada semana si te sientes bien.")
        )
    }

    fun advice(points: List<PainPoint>): String {
        if (points.isEmpty()) return "Sin dolores registrados hoy. ¡Sigue así! Mantén actividad física suave y buena postura."
        val sb = StringBuilder()
        val max = points.maxBy { it.score }
        if (max.score >= 8) sb.append("⚠️ Has registrado un dolor intenso (${max.score}/10) en ${Zones.byId(max.zoneId).name.lowercase()}. Si persiste más de unos días, empeora o se acompaña de fiebre, hormigueo o pérdida de fuerza, acude a un profesional sanitario.\n\n")
        points.sortedByDescending { it.score }.forEach { p ->
            val z = Zones.byId(p.zoneId)
            val level = when { p.score >= 7 -> "intenso"; p.score >= 4 -> "moderado"; else -> "leve" }
            sb.append("• ${z.name}: dolor $level (${p.score}/10). ")
            sb.append(when {
                p.score >= 7 -> "Prioriza reposo relativo, aplica frío 15 min si hay inflamación reciente o calor si es tensión muscular, y realiza solo los ejercicios más suaves sin forzar."
                p.score >= 4 -> "Realiza los ejercicios recomendados 1-2 veces al día, evita posturas mantenidas y haz pausas activas cada hora."
                else -> "Buen momento para trabajar movilidad y fortalecimiento progresivo de la zona."
            })
            sb.append("\n")
        }
        sb.append("\nConsejo general: hidrátate bien, duerme 7-9 horas y evita estar más de 1 hora seguida en la misma postura.")
        return sb.toString()
    }
}
