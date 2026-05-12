package com.project.weathertec.data.model

/**
 * Modelo que refleja exactamente la estructura guardada en Firebase por la web:
 * environmentalData/{date}/{HH-mm} -> { date, time, temperature, humidity, windSpeed, conditions, icon, source, timestamp }
 */
data class WeatherRecord(
    val date: String = "",
    val time: String = "",
    val temperature: Double? = null,
    val humidity: Double? = null,
    val windSpeed: Double? = null,
    val conditions: String = "",
    val icon: String = "",
    val source: String = "google_weather",
    val timestamp: String = ""
)
