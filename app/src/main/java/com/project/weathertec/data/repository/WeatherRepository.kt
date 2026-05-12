package com.project.weathertec.data.repository

import com.project.weathertec.BuildConfig
import com.project.weathertec.data.model.WeatherRecord
import com.project.weathertec.data.remote.RetrofitClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRepository {

    private val googleApi = RetrofitClient.googleWeatherApi
    private val firebaseApi = RetrofitClient.firebaseApi

    companion object {
        const val LAT = 10.3643
        const val LON = -84.5097
    }

    // Fetch live conditions from Google Weather API
    suspend fun fetchLiveConditions(): WeatherRecord? {
        return try {
            val response = googleApi.getCurrentConditions(
                apiKey = BuildConfig.GOOGLE_WEATHER_KEY,
                lat = LAT,
                lon = LON
            )
            val now = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val record = WeatherRecord(
                date = dateFormat.format(now),
                time = timeFormat.format(now),
                temperature = response.temperature?.degrees,
                humidity = response.relativeHumidity,
                windSpeed = response.wind?.speed?.value,
                conditions = response.weatherCondition?.description?.text ?: "",
                icon = response.weatherCondition?.iconBaseUri ?: "",
                source = "google_weather",
                timestamp = now.toInstant().toString()
            )
            saveToFirebase(record)
            record
        } catch (e: Exception) {
            null
        }
    }

    // Save record to Firebase
    private suspend fun saveToFirebase(record: WeatherRecord) {
        try {
            val timeKey = record.time.replace(":", "-")
            val body = mapOf<String, Any?>(
                "date" to record.date,
                "time" to record.time,
                "temperature" to record.temperature,
                "humidity" to record.humidity,
                "windSpeed" to record.windSpeed,
                "conditions" to record.conditions,
                "icon" to record.icon,
                "source" to record.source,
                "timestamp" to record.timestamp
            )
            firebaseApi.saveRecord(record.date, timeKey, body)
        } catch (_: Exception) {
            // Silent fail — don't block UI
        }
    }

    // Fetch historical records from Firebase for a date range
    suspend fun fetchFromFirebase(startDate: String, endDate: String? = null): List<WeatherRecord> {
        return try {
            val raw = firebaseApi.getAllData() ?: return emptyList()
            val records = mutableListOf<WeatherRecord>()

            for ((date, hoursElement) in raw.entrySet()) {
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
                            temperature = obj.get("temperature")?.asDouble,
                            humidity = obj.get("humidity")?.asDouble,
                            windSpeed = obj.get("windSpeed")?.asDouble,
                            conditions = obj.get("conditions")?.asString ?: "",
                            icon = obj.get("icon")?.asString ?: "",
                            source = obj.get("source")?.asString ?: "",
                            timestamp = obj.get("timestamp")?.asString ?: ""
                        )
                    )
                }
            }
            records.sortedBy { it.timestamp }
        } catch (e: Exception) {
            throw e
        }
    }
}
