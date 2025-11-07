package com.tryon.virtualfit.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * Retrofit API interface for Virtual Try-On service
 * Gradio 4+ uses a three-step process: upload + queue + poll
 */
interface TryOnApiService {

    /**
     * Step 0: Upload files to Gradio
     * @param file The file to upload
     * @return Response with file paths
     */
    @Multipart
    @POST("/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<List<String>>

    /**
     * Step 1: Queue the prediction request
     * @param request The Gradio request containing the data
     * @return Response with event_id
     */
    @POST("/queue/join")
    suspend fun queuePrediction(
        @Body request: GradioRequest
    ): Response<GradioQueueResponse>

    /**
     * Step 2: Get the prediction result using server-sent events
     * @param sessionHash The session hash used in queue join
     * @return Response containing the result stream
     */
    @GET("/queue/data")
    @Streaming
    suspend fun getPredictionResult(
        @retrofit2.http.Query("session_hash") sessionHash: String
    ): Response<ResponseBody>
}

/**
 * Gradio API request format for gr.Interface (Gradio v4)
 * Images should be sent as file data objects
 */
data class GradioRequest(
    val data: List<Any>,
    val event_data: Any? = null,
    val fn_index: Int? = null,
    val trigger_id: Int? = null,
    val session_hash: String,
    val api_name: String? = null
)

/**
 * Gradio v5 image data format
 */
data class GradioImageData(
    val path: String? = null,
    val url: String,  // Base64 data URL
    val orig_name: String = "image.jpg",
    val size: Int? = null,
    val mime_type: String = "image/jpeg",
    val is_stream: Boolean = false,
    val meta: Map<String, String> = mapOf("_type" to "gradio.FileData")
)

/**
 * Gradio queue response (Step 1)
 * Returns event_id to poll for results
 */
data class GradioQueueResponse(
    val event_id: String
)
