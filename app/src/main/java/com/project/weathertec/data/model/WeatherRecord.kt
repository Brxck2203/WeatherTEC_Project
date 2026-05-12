package com.project.weathertec.data.model

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
