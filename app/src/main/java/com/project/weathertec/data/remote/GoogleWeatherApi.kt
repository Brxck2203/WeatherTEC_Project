package com.project.weathertec.data.remote

import com.project.weathertec.data.model.GoogleWeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleWeatherApi {
    @GET("v1/currentConditions:lookup")
    suspend fun getCurrentConditions(
        @Query("key") apiKey: String,
        @Query("location.latitude") lat: Double,
        @Query("location.longitude") lon: Double
    ): GoogleWeatherResponse
}
