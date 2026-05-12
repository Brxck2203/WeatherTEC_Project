package com.project.weathertec.data.remote

import com.google.gson.JsonObject
import retrofit2.http.GET

interface FirebaseApi {
    /**
     * Lee el nodo completo environmentalData de Firebase.
     * La escritura la hace exclusivamente la página web.
     */
    @GET("environmentalData.json")
    suspend fun getAllData(): JsonObject?
}
