package com.kisahcode.machinelearningandroid.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton object responsible for configuring Retrofit and providing an instance of ApiService.
 */
object ApiConfig {

    /**
     * Retrieves an instance of ApiService for interacting with the remote server.
     *
     * @return ApiService: An instance of ApiService.
     */
    fun getApiService(): ApiService {

        // Create a logging interceptor for debugging network requests and responses.
        val loggingInterceptor =
            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

        // Create an OkHttpClient instance with the logging interceptor.
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        // Create a Retrofit instance with base URL, Gson converter factory, and OkHttpClient.
        val retrofit = Retrofit.Builder()
            .baseUrl("https://classification-api.dicoding.dev/") // Base URL of the API
            .addConverterFactory(GsonConverterFactory.create()) // JSON converter using Gson
            .client(client) // OkHttpClient for handling network requests
            .build()

        // Create and return an instance of ApiService using Retrofit.
        return retrofit.create(ApiService::class.java)
    }
}