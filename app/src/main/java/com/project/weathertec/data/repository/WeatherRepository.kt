package com.project.weathertec.data.repository

import com.project.weathertec.data.model.WeatherRecord
import com.project.weathertec.data.remote.RetrofitClient

class WeatherRepository {

    private val firebaseApi = RetrofitClient.firebaseApi

    /**
     * Lee TODOS los registros de Firebase y retorna el más reciente de hoy.
     * La app NUNCA llama a Google Weather — eso es responsabilidad exclusiva de la web.
     */
    suspend fun fetchLatestRecord(): WeatherRecord? {
        return try {
            val today = todayDate()
            val records = fetchFromFirebase(today)
            records.lastOrNull() // ya vienen ordenados por timestamp
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Lee registros de Firebase filtrados por fecha o rango de fechas.
     * @param startDate Fecha inicio en formato YYYY-MM-DD
     * @param endDate   Fecha fin en formato YYYY-MM-DD (opcional, si null = solo startDate)
     */
    suspend fun fetchFromFirebase(startDate: String, endDate: String? = null): List<WeatherRecord> {
        val raw = firebaseApi.getAllData() ?: return emptyList()
        val records = mutableListOf<WeatherRecord>()

        for ((date, hoursElement) in raw.entrySet()) {
            // Filtrar por rango
            if (endDate != null) {
                if (date < startDate || date > endDate) continue
            } else {
                if (date != startDate) continue
            }

            if (!hoursElement.isJsonObject) continue
            val hours = hoursElement.asJsonObject

            for ((timeKey, valueElement) in hours.entrySet()) {
                if (!valueElement.isJsonObject) continue
                val obj = valueElement.asJsonObject

                records.add(
                    WeatherRecord(
                        date = obj.get("date")?.asString ?: date,
                        time = obj.get("time")?.asString ?: timeKey.replace("-", ":"),
                        temperature = obj.get("temperature")?.takeIf { !it.isJsonNull }?.asDouble,
                        humidity = obj.get("humidity")?.takeIf { !it.isJsonNull }?.asDouble,
                        windSpeed = obj.get("windSpeed")?.takeIf { !it.isJsonNull }?.asDouble,
                        conditions = obj.get("conditions")?.asString ?: "",
                        icon = obj.get("icon")?.asString ?: "",
                        source = obj.get("source")?.asString ?: "google_weather",
                        timestamp = obj.get("timestamp")?.asString ?: ""
                    )
                )
            }
        }

        // Ordenar igual que la web: por timestamp ascendente
        return records.sortedBy { it.timestamp }
    }

    private fun todayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
