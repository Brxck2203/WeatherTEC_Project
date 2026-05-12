package com.project.weathertec.data.remote

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface FirebaseApi {
    @GET("environmentalData.json")
    suspend fun getAllData(): JsonObject?

    @PUT("environmentalData/{date}/{timeKey}.json")
    suspend fun saveRecord(
        @Path("date") date: String,
        @Path("timeKey") timeKey: String,
        @Body record: Map<String, Any?>
    ): JsonObject?
}
