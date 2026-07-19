package com.xito.fixio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Achievement(val id: String, val emoji: String, val title: String, val desc: String)

object Achievements {
    val all = listOf(
        Achievement("first", "🌱", "Primer paso", "Completa tu primer registro"),
        Achievement("streak3", "🔥", "En marcha", "3 días seguidos registrando"),
        Achievement("streak7", "⭐", "Semana perfecta", "7 días de racha"),
        Achievement("streak30", "🏆", "Constancia de hierro", "30 días de racha"),
        Achievement("records10", "📚", "Diario fiel", "10 registros totales"),
        Achievement("painfree", "🎉", "Día sin dolor", "Un día con 0 zonas de dolor"),
        Achievement("improve", "📉", "A mejor", "Baja tu dolor medio respecto a la semana previa"),
        Achievement("exercise5", "💪", "En forma", "Completa 5 ejercicios"),
        Achievement("exercise25", "🥇", "Atleta", "Completa 25 ejercicios"),
        Achievement("night", "🌙", "Búho registrador", "Registra después de las 23:00")
    )

    fun unlocked(c: Context): Set<String> {
        val e = Store.entries(c)
        val out = mutableSetOf<String>()
        if (e.isNotEmpty()) out.add("first")
        if (Store.streak(c) >= 3) out.add("streak3")
        if (Store.streak(c) >= 7) out.add("streak7")
        if (Store.streak(c) >= 30) out.add("streak30")
        if (e.size >= 10) out.add("records10")
        if (e.any { it.points.isEmpty() }) out.add("painfree")
        val prev = e.dropLast(7).takeLast(7).flatMap { it.points }.map { it.score }
        val now = e.takeLast(7).flatMap { it.points }.map { it.score }
        if (prev.isNotEmpty() && now.isNotEmpty() && now.average() < prev.average()) out.add("improve")
        val done = ExtraStore.exercisesDone(c)
        if (done >= 5) out.add("exercise5")
        if (done >= 25) out.add("exercise25")
        if (ExtraStore.registeredLate(c)) out.add("night")
        return out
    }
}

data class Medication(val id: String, val name: String, val dose: String, val schedule: String, val active: Boolean)

object ExtraStore {
    private fun p(c: Context) = c.getSharedPreferences("fixio", Context.MODE_PRIVATE)

    fun exercisesDone(c: Context) = p(c).getInt("exDone", 0)
    fun addExerciseDone(c: Context) = p(c).edit().putInt("exDone", exercisesDone(c) + 1).apply()

    fun markRegisteredLate(c: Context) {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (h >= 23) p(c).edit().putBoolean("lateReg", true).apply()
    }
    fun registeredLate(c: Context) = p(c).getBoolean("lateReg", false)

    // completed exercises per date: set of keys
    fun doneToday(c: Context): MutableSet<String> {
        val raw = p(c).getString("doneEx_${Store.today()}", "") ?: ""
        return if (raw.isBlank()) mutableSetOf() else raw.split("|").toMutableSet()
    }
    fun toggleDone(c: Context, key: String) {
        val s = doneToday(c)
        if (s.contains(key)) s.remove(key) else { s.add(key); addExerciseDone(c) }
        p(c).edit().putString("doneEx_${Store.today()}", s.joinToString("|")).apply()
    }

    fun meds(c: Context): List<Medication> {
        val arr = JSONArray(p(c).getString("meds", "[]"))
        val out = ArrayList<Medication>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(Medication(o.getString("id"), o.getString("name"), o.optString("dose"), o.optString("sch"), o.optBoolean("act", true)))
        }
        return out
    }
    fun saveMed(c: Context, m: Medication) {
        val list = meds(c).filter { it.id != m.id } + m
        writeMeds(c, list)
    }
    fun deleteMed(c: Context, id: String) = writeMeds(c, meds(c).filter { it.id != id })
    private fun writeMeds(c: Context, list: List<Medication>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().put("id", m.id).put("name", m.name).put("dose", m.dose).put("sch", m.schedule).put("act", m.active))
        }
        p(c).edit().putString("meds", arr.toString()).apply()
    }
    fun medTakenToday(c: Context, id: String): Boolean {
        val raw = p(c).getString("medTaken_${Store.today()}", "") ?: ""
        return raw.split("|").contains(id)
    }
    fun toggleMedTaken(c: Context, id: String) {
        val key = "medTaken_${Store.today()}"
        val s = (p(c).getString(key, "") ?: "").split("|").filter { it.isNotBlank() }.toMutableSet()
        if (s.contains(id)) s.remove(id) else s.add(id)
        p(c).edit().putString(key, s.joinToString("|")).apply()
    }
}

object Report {
    fun forDoctor(c: Context): String {
        val e = Store.entries(c)
        val sb = StringBuilder()
        sb.append("INFORME FIXIO — ${Store.name(c)}\n")
        sb.append("Generado: ${SimpleDateFormat("dd/MM/yyyy", Locale("es")).format(Date())}\n")
        sb.append("Periodo: ${e.firstOrNull()?.date ?: "-"} a ${e.lastOrNull()?.date ?: "-"} (${e.size} registros)\n")
        sb.append("NOTA: datos autorreportados por el usuario. No constituye diagnóstico médico.\n\n")

        val freq = e.flatMap { it.points }.groupBy { it.zoneId }
            .mapValues { (_, v) -> Triple(v.size, v.map { it.score }.average(), v.maxOf { it.score }) }
            .entries.sortedByDescending { it.value.first }
        sb.append("ZONAS AFECTADAS (frecuencia · media · máx):\n")
        if (freq.isEmpty()) sb.append("  Ninguna registrada.\n")
        freq.forEach { (z, t) ->
            sb.append("  • ${Zones.byId(z).name}: ${t.first} días · media ${String.format(Locale.US, "%.1f", t.second)}/10 · máx ${t.third}/10\n")
        }
        val avgMood = e.map { it.mood }.average()
        val avgSleep = e.map { it.sleep }.average()
        sb.append("\nBIENESTAR MEDIO: ánimo ${String.format(Locale.US, "%.1f", avgMood)}/5 · sueño ${String.format(Locale.US, "%.1f", avgSleep)}/5\n")

        val meds = ExtraStore.meds(c).filter { it.active }
        if (meds.isNotEmpty()) {
            sb.append("\nTRATAMIENTOS EN CURSO:\n")
            meds.forEach { sb.append("  • ${it.name} ${it.dose} — ${it.schedule}\n") }
        }
        sb.append("\nÚLTIMOS 7 DÍAS:\n")
        e.takeLast(7).forEach { d ->
            sb.append("  ${d.date}: ")
            sb.append(if (d.points.isEmpty()) "sin dolor" else d.points.sortedByDescending { it.score }.joinToString(", ") { "${Zones.byId(it.zoneId).name} ${it.score}/10" })
            sb.append("\n")
        }
        return sb.toString()
    }
}
