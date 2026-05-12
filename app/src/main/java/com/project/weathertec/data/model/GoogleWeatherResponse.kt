package com.project.weathertec.data.model

import com.google.gson.annotations.SerializedName

data class GoogleWeatherResponse(
    @SerializedName("temperature") val temperature: Temperature? = null,
    @SerializedName("relativeHumidity") val relativeHumidity: Double? = null,
    @SerializedName("wind") val wind: Wind? = null,
    @SerializedName("weatherCondition") val weatherCondition: WeatherCondition? = null
)

data class Temperature(
    @SerializedName("degrees") val degrees: Double? = null
)

data class Wind(
    @SerializedName("speed") val speed: WindSpeed? = null
)

data class WindSpeed(
    @SerializedName("value") val value: Double? = null
)

data class WeatherCondition(
    @SerializedName("description") val description: ConditionDescription? = null,
    @SerializedName("iconBaseUri") val iconBaseUri: String? = null
)

data class ConditionDescription(
    @SerializedName("text") val text: String? = null
)
