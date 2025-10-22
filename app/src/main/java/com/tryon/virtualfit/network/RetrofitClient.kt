package com.tryon.virtualfit.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client for API calls
 */
object RetrofitClient {

    private var baseUrl: String = ""
    private var retrofit: Retrofit? = null

    /**
     * Set the base URL for the API (Gradio endpoint)
     */
    fun setBaseUrl(url: String) {
        val formattedUrl = if (!url.endsWith("/")) "$url/" else url
        if (baseUrl != formattedUrl) {
            baseUrl = formattedUrl
            retrofit = null // Reset retrofit instance
        }
    }

    /**
     * Get the TryOnApiService instance
     */
    fun getApiService(): TryOnApiService {
        if (retrofit == null && baseUrl.isNotEmpty()) {
            retrofit = createRetrofit()
        }
        return retrofit?.create(TryOnApiService::class.java)
            ?: throw IllegalStateException("Base URL not set. Call setBaseUrl() first.")
    }

    private fun createRetrofit(): Retrofit {
        // Logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // OkHttp client with extended timeouts for CPU processing
        // CPU processing takes 3-5 minutes, so we need longer timeouts
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(120, TimeUnit.SECONDS)  // 2 minutes to connect
            .readTimeout(600, TimeUnit.SECONDS)     // 10 minutes (CPU is very slow!)
            .writeTimeout(600, TimeUnit.SECONDS)    // 10 minutes
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun isBaseUrlSet(): Boolean = baseUrl.isNotEmpty()
}
