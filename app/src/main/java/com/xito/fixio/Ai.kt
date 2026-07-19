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
                val prompt = "Eres un asistente de bienestar físico (NO médico; recuérdalo brevemente). Usuario registra hoy:\n$desc\nÁnimo ${entry.mood}/5, sueño ${entry.sleep}/5, actividad ${entry.activity}/5.\nHistorial 7 días:\n$hist\n\nEn español, breve y práctico: 1) posible origen habitual de estas molestias, 2) consejos concretos para hoy, 3) señales de alarma para acudir al médico. Máximo 250 palabras."
                val body = JSONObject()
                    .put("model", "claude-sonnet-4-6")
                    .put("max_tokens", 800)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                    .toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", key)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(body)
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
