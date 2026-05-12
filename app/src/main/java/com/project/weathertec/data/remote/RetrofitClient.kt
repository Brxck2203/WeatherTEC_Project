package com.project.weathertec.data.remote

import com.project.weathertec.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Cliente HTTP solo para Firebase — la app no necesita llamar a Google
    val firebaseApi: FirebaseApi = Retrofit.Builder()
        .baseUrl(BuildConfig.FIREBASE_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FirebaseApi::class.java)
}
