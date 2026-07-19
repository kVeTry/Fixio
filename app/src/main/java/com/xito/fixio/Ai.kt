package com.xito.fixio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Ai {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun diagnose(c: Context, entry: DayEntry, history: List<DayEntry>): String {
        val key = Store.apiKey(c)
        if (key.isBlank()) return ExerciseDb.advice(entry.points)
        return withContext(Dispatchers.IO) {
            try {
                val desc = entry.points.joinToString("\n") { p ->
                    "- ${Zones.byId(p.zoneId).name}: ${p.score}/10" + if (p.note.isNotBlank()) " (nota: ${p.note})" else ""
                }
                val hist = history.takeLast(7).joinToString("\n") { d ->
                    "${d.date}: " + if (d.points.isEmpty()) "sin dolor" else d.points.joinToString(", ") { "${Zones.byId(it.zoneId).name} ${it.score}/10" }
                }
                val profileBody = buildString {
                    if (Store.age(c) > 0) append("Edad ${Store.age(c)}. ")
                    if (Store.heightCm(c) > 0) append("Altura ${Store.heightCm(c)}cm. ")
                    if (Store.weightKg(c) > 0) append("Peso ${Store.weightKg(c)}kg. ")
                    Store.bmi(c)?.let { append("IMC ${String.format(java.util.Locale.US, "%.1f", it)}. ") }
                }
                val prompt = "Eres un asistente de bienestar físico (NO médico; recuérdalo brevemente). Perfil: $profileBody Usuario registra hoy:\n$desc\nÁnimo ${entry.mood}/5, sueño ${entry.sleep}/5, actividad ${entry.activity}/5.\nÚltimos 7 días:\n$hist\n\nDa un breve análisis personalizado (máx 3 párrafos), consejos y recordatorio de que NO reemplaza médico."
                val requestBody = JSONObject()
                    .put("model", "claude-sonnet-4-6")
                    .put("max_tokens", 800)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                    .toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", key)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(requestBody)
                    .build()
                client.newCall(req).execute().use { resp ->
                    val txt = resp.body?.string() ?: ""
                    if (!resp.isSuccessful) return@withContext "Error de la IA (${resp.code}). Consejo local:\n\n" + ExerciseDb.advice(entry.points)
                    val content = JSONObject(txt).getJSONArray("content")
                    val sb = StringBuilder()
                    for (i in 0 until content.length()) {
                        val o = content.getJSONObject(i)
                        if (o.getString("type") == "text") sb.append(o.getString("text"))
                    }
                    sb.toString().ifBlank { ExerciseDb.advice(entry.points) }
                }
            } catch (e: Exception) {
                "Sin conexión con la IA. Consejo local:\n\n" + ExerciseDb.advice(entry.points)
            }
        }
    }
}
