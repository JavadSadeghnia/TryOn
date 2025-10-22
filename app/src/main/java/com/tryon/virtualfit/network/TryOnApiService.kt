package com.tryon.virtualfit.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * Retrofit API interface for Virtual Try-On service
 * Gradio 4+ uses a two-step process: call + poll
 */
interface TryOnApiService {

    /**
     * Step 1: Queue the prediction request
     * @param request The Gradio request containing the data
     * @return Response with event_id
     */
    @POST("/gradio_api/call/predict")
    suspend fun queuePrediction(
        @Body request: GradioRequest
    ): Response<GradioQueueResponse>

    /**
     * Step 2: Get the prediction result using server-sent events
     * @param eventId The event ID from step 1
     * @return Response containing the result
     */
    @GET("/gradio_api/call/predict/{event_id}")
    @Streaming
    suspend fun getPredictionResult(
        @Path("event_id") eventId: String
    ): Response<ResponseBody>
}

/**
 * Gradio API request format for gr.Interface (Gradio v5)
 * Images should be sent as file data objects
 */
data class GradioRequest(
    val data: List<GradioImageData>
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
