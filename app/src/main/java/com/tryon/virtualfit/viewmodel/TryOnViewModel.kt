package com.tryon.virtualfit.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tryon.virtualfit.data.TryOnResult
import com.tryon.virtualfit.network.ApiConfig
import com.tryon.virtualfit.network.GradioRequest
import com.tryon.virtualfit.network.RetrofitClient
import com.tryon.virtualfit.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * ViewModel for managing Virtual Try-On app state
 */
class TryOnViewModel : ViewModel() {

    companion object {
        private const val TAG = "TryOnViewModel"
    }

    // UI State
    private val _bodyImageUri = MutableStateFlow<Uri?>(null)
    val bodyImageUri: StateFlow<Uri?> = _bodyImageUri.asStateFlow()

    private val _clothingImageUri = MutableStateFlow<Uri?>(null)
    val clothingImageUri: StateFlow<Uri?> = _clothingImageUri.asStateFlow()

    private val _tryOnResult = MutableStateFlow<TryOnResult>(TryOnResult.Idle)
    val tryOnResult: StateFlow<TryOnResult> = _tryOnResult.asStateFlow()

    private val _apiUrl = MutableStateFlow(ApiConfig.DEFAULT_API_URL)
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    init {
        // Set default API URL on initialization
        if (ApiConfig.DEFAULT_API_URL.isNotEmpty()) {
            setApiUrl(ApiConfig.DEFAULT_API_URL)
            Log.d(TAG, "Using default API URL: ${ApiConfig.DEFAULT_API_URL}")
        }
    }

    /**
     * Set the body image URI
     */
    fun setBodyImage(uri: Uri?) {
        _bodyImageUri.value = uri
        // Reset result when changing images
        if (_tryOnResult.value !is TryOnResult.Loading) {
            _tryOnResult.value = TryOnResult.Idle
        }
    }

    /**
     * Set the clothing image URI
     */
    fun setClothingImage(uri: Uri?) {
        _clothingImageUri.value = uri
        // Reset result when changing images
        if (_tryOnResult.value !is TryOnResult.Loading) {
            _tryOnResult.value = TryOnResult.Idle
        }
    }

    /**
     * Set the clothing image from a drawable resource
     */
    fun setClothingImageFromResource(context: Context, resourceId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading clothing from resource ID: $resourceId")

                // Decode drawable resource to bitmap, then save to temporary file
                val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, resourceId)
                if (bitmap == null) {
                    Log.e(TAG, "Failed to decode drawable resource: $resourceId")
                    withContext(Dispatchers.Main) {
                        _tryOnResult.value = TryOnResult.Error("Failed to load sample image")
                    }
                    return@launch
                }

                Log.d(TAG, "Bitmap loaded: width=${bitmap.width}, height=${bitmap.height}, config=${bitmap.config}")

                // Create a persistent temp file (not using createTempFile which may get auto-deleted)
                val fileName = "sample_clothing_${System.currentTimeMillis()}.jpg"
                val tempFile = File(context.cacheDir, fileName)

                val compressed = tempFile.outputStream().use { output ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, output)
                }

                if (!compressed) {
                    Log.e(TAG, "Failed to compress bitmap to JPEG")
                    bitmap.recycle()
                    withContext(Dispatchers.Main) {
                        _tryOnResult.value = TryOnResult.Error("Failed to process sample image")
                    }
                    return@launch
                }

                bitmap.recycle()

                // Verify the file was written correctly
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    Log.e(TAG, "Temp file is empty or doesn't exist: exists=${tempFile.exists()}, size=${tempFile.length()}")
                    withContext(Dispatchers.Main) {
                        _tryOnResult.value = TryOnResult.Error("Failed to save sample image")
                    }
                    return@launch
                }

                Log.d(TAG, "Created temp file from resource: ${tempFile.absolutePath}, size=${tempFile.length()} bytes, exists=${tempFile.exists()}")

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )

                Log.d(TAG, "Sample image URI created: $uri")

                // Double-check file still exists after creating URI
                Log.d(TAG, "File check after URI creation: exists=${tempFile.exists()}, size=${tempFile.length()}")

                withContext(Dispatchers.Main) {
                    setClothingImage(uri)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading resource image", e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _tryOnResult.value = TryOnResult.Error("Failed to load sample image: ${e.message}")
                }
            }
        }
    }

    /**
     * Set and save the API URL
     */
    fun setApiUrl(url: String) {
        _apiUrl.value = url
        if (url.isNotEmpty()) {
            RetrofitClient.setBaseUrl(url)
        }
    }

    /**
     * Generate the try-on image
     */
    fun generateTryOn(context: Context) {
        val bodyUri = _bodyImageUri.value
        val clothingUri = _clothingImageUri.value

        if (bodyUri == null || clothingUri == null) {
            _tryOnResult.value = TryOnResult.Error("Please select both images")
            return
        }

        if (!RetrofitClient.isBaseUrlSet()) {
            _tryOnResult.value = TryOnResult.Error("Please set the API URL first")
            return
        }

        viewModelScope.launch {
            try {
                _tryOnResult.value = TryOnResult.Loading
                Log.d(TAG, "Starting try-on generation")

                // Convert URIs to files
                val bodyFile = FileUtils.getFileFromUri(context, bodyUri)
                val clothingFile = FileUtils.getFileFromUri(context, clothingUri)

                if (bodyFile == null || clothingFile == null) {
                    _tryOnResult.value = TryOnResult.Error("Failed to process images")
                    return@launch
                }

                Log.d(TAG, "Files obtained: body=${bodyFile.name}, clothing=${clothingFile.name}")

                // Read files and convert to base64 data URLs (Gradio format)
                val bodyBytes = bodyFile.readBytes()
                val clothingBytes = clothingFile.readBytes()

                Log.d(TAG, "Image sizes: body=${bodyBytes.size}, clothing=${clothingBytes.size}")

                val bodyBase64 = Base64.encodeToString(bodyBytes, Base64.NO_WRAP)
                val clothingBase64 = Base64.encodeToString(clothingBytes, Base64.NO_WRAP)

                Log.d(TAG, "Uploading images to Gradio server...")

                // Validate image sizes
                if (bodyBytes.isEmpty() || clothingBytes.isEmpty()) {
                    Log.e(TAG, "One or both images are empty: body=${bodyBytes.size}, clothing=${clothingBytes.size}")
                    _tryOnResult.value = TryOnResult.Error("Invalid image files")
                    return@launch
                }

                // Step 0: Upload files to Gradio
                val apiService = RetrofitClient.getApiService()

                // Upload body image
                val bodyRequestBody = bodyBytes.toRequestBody("image/jpeg".toMediaType())
                val bodyPart = okhttp3.MultipartBody.Part.createFormData(
                    "files",
                    "body.jpg",
                    bodyRequestBody
                )

                val bodyUploadResponse = apiService.uploadFile(bodyPart)
                if (!bodyUploadResponse.isSuccessful || bodyUploadResponse.body() == null) {
                    Log.e(TAG, "Body image upload failed: ${bodyUploadResponse.code()}")
                    _tryOnResult.value = TryOnResult.Error("Failed to upload body image")
                    return@launch
                }
                val bodyFilePath = bodyUploadResponse.body()!![0]
                Log.d(TAG, "Body image uploaded: $bodyFilePath")

                // Upload clothing image
                val clothingRequestBody = clothingBytes.toRequestBody("image/jpeg".toMediaType())
                val clothingPart = okhttp3.MultipartBody.Part.createFormData(
                    "files",
                    "clothing.jpg",
                    clothingRequestBody
                )

                val clothingUploadResponse = apiService.uploadFile(clothingPart)
                if (!clothingUploadResponse.isSuccessful || clothingUploadResponse.body() == null) {
                    Log.e(TAG, "Clothing image upload failed: ${clothingUploadResponse.code()}")
                    _tryOnResult.value = TryOnResult.Error("Failed to upload clothing image")
                    return@launch
                }
                val clothingFilePath = clothingUploadResponse.body()!![0]
                Log.d(TAG, "Clothing image uploaded: $clothingFilePath")

                // Generate a unique session hash for this request
                val sessionHash = java.util.UUID.randomUUID().toString().replace("-", "")

                // OOTDiffusion Half-body mode API format - 6 parameters needed!
                // Build full URLs for the files (matching browser format exactly)
                val baseUrl = _apiUrl.value.removeSuffix("/")
                val bodyFileUrl = "$baseUrl/file=$bodyFilePath"
                val clothingFileUrl = "$baseUrl/file=$clothingFilePath"

                // Create FileData dict for body/model image (parameter 1)
                // Must match browser format exactly: url field + null for size/mime_type
                val bodyFileData = mapOf(
                    "path" to bodyFilePath,
                    "url" to bodyFileUrl,
                    "orig_name" to "body.jpg",
                    "size" to null,
                    "mime_type" to null
                )

                // Create FileData dict for clothing/garment image (parameter 2)
                val clothingFileData = mapOf(
                    "path" to clothingFilePath,
                    "url" to clothingFileUrl,
                    "orig_name" to "clothing.jpg",
                    "size" to null,
                    "mime_type" to null
                )

                val request = GradioRequest(
                    data = listOf(
                        bodyFileData,              // 1. Model image (FileData dict)
                        clothingFileData,          // 2. Garment image (FileData dict)
                        1,                         // 3. Images (int: 1-4)
                        20,                        // 4. Steps (int: 10-40)
                        2,                         // 5. Guidance scale (int: 1-5)
                        42                         // 6. Seed (int: -1 to 2147483647)
                    ),
                    event_data = null,
                    fn_index = 2,              // Half-body is fn_index 2 (examples take 0 and 1)
                    trigger_id = 17,           // Trigger ID from browser
                    session_hash = sessionHash
                )

                Log.d(TAG, "Files uploaded - body: $bodyFilePath, clothing: $clothingFilePath")
                Log.d(TAG, "Session hash: $sessionHash")

                Log.d(TAG, "Sending request to Gradio API (Step 1: Queue)")

                // Step 1: Queue the prediction
                val queueResponse = apiService.queuePrediction(request)

                Log.d(TAG, "Queue response body: ${queueResponse.body()}")

                Log.d(TAG, "Queue response: code=${queueResponse.code()}, success=${queueResponse.isSuccessful}")

                if (!queueResponse.isSuccessful) {
                    val errorBody = queueResponse.errorBody()?.string()
                    Log.e(TAG, "Queue error: ${queueResponse.code()} - ${queueResponse.message()}, body: $errorBody")

                    val errorMessage = when (queueResponse.code()) {
                        429 -> "Rate limit reached. Please wait a minute and try again."
                        503 -> "AI service is busy. Please try again in a moment."
                        500 -> "Server error. The AI service may be temporarily down."
                        404 -> "API endpoint not found. Please check your URL."
                        else -> "Server error: ${queueResponse.code()} - ${queueResponse.message()}"
                    }
                    _tryOnResult.value = TryOnResult.Error(errorMessage)
                    return@launch
                }

                val eventId = queueResponse.body()?.event_id
                if (eventId == null) {
                    _tryOnResult.value = TryOnResult.Error("No event ID received")
                    return@launch
                }

                Log.d(TAG, "Event ID received: $eventId, polling for result (Step 2)")

                // Step 2: Poll for the result using SSE with session_hash
                val resultResponse = apiService.getPredictionResult(sessionHash)

                Log.d(TAG, "Result response: code=${resultResponse.code()}, success=${resultResponse.isSuccessful}")

                if (resultResponse.isSuccessful) {
                    val responseBody = resultResponse.body()
                    val resultImageUrl = parseServerSentEvents(responseBody)

                    if (resultImageUrl != null) {
                        Log.d(TAG, "Result image URL: $resultImageUrl")

                        // If it's a data URL, decode it
                        val imageData = if (resultImageUrl.startsWith("data:")) {
                            val base64Data = resultImageUrl.substringAfter("base64,")
                            Base64.decode(base64Data, Base64.DEFAULT)
                        } else {
                            // If it's a URL or path, download it
                            withContext(Dispatchers.IO) {
                                downloadImageFromUrl(resultImageUrl)
                            }
                        }
                        Log.d(TAG, "Image data obtained: size=${imageData.size}")
                        _tryOnResult.value = TryOnResult.Success(imageData)
                    } else {
                        _tryOnResult.value = TryOnResult.Error("Empty response from server")
                    }
                } else {
                    val errorBody = resultResponse.errorBody()?.string()
                    Log.e(TAG, "Result error: ${resultResponse.code()} - ${resultResponse.message()}, body: $errorBody")

                    // Provide helpful error messages based on status code
                    val errorMessage = when (resultResponse.code()) {
                        429 -> "Rate limit reached. Please wait a minute and try again."
                        503 -> "AI service is busy. Please try again in a moment."
                        500 -> "Server error. The AI service may be temporarily down."
                        404 -> "API endpoint not found. Please check your URL."
                        else -> "Server error: ${resultResponse.code()} - ${resultResponse.message()}"
                    }

                    _tryOnResult.value = TryOnResult.Error(errorMessage)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception occurred", e)
                e.printStackTrace()
                val errorMessage = when (e) {
                    is java.net.UnknownHostException -> {
                        Log.e(TAG, "DNS resolution failed for: ${_apiUrl.value}")
                        "Network error: Cannot reach ${_apiUrl.value}\n\n" +
                        "Possible causes:\n" +
                        "• No internet connection\n" +
                        "• Emulator DNS issue (try restarting)\n" +
                        "• Try on a real device\n\n" +
                        "Your Space URL: ${_apiUrl.value}"
                    }
                    is java.net.SocketTimeoutException -> "Timeout: AI is taking too long. The service may be busy. Please try again."
                    is java.io.IOException -> "Network error: ${e.message}"
                    is com.google.gson.JsonSyntaxException -> "Invalid response from server. The AI service may be having issues."
                    else -> {
                        val msg = e.message ?: "Unknown error"
                        if (msg.contains("429") || msg.contains("rate limit", ignoreCase = true)) {
                            "Rate limit reached. Please wait a minute and try again."
                        } else if (msg.contains("503") || msg.contains("busy", ignoreCase = true)) {
                            "AI service is busy. Please try again in a moment."
                        } else if (msg.contains("unavailable", ignoreCase = true)) {
                            "The AI service is currently unavailable.\n\n" +
                            "Your HuggingFace Space is calling external APIs that are rate-limited.\n\n" +
                            "Wait 2-3 minutes and try again."
                        } else {
                            "Error: ${msg}\n\nPlease try again."
                        }
                    }
                }
                _tryOnResult.value = TryOnResult.Error(errorMessage)
            }
        }
    }

    /**
     * Parse Server-Sent Events (SSE) from Gradio API
     * Returns the image URL from the completed event
     */
    private fun parseServerSentEvents(responseBody: okhttp3.ResponseBody?): String? {
        if (responseBody == null) return null

        var responseString = ""
        try {
            responseString = responseBody.string()
            Log.d(TAG, "SSE Full Response: $responseString")
            Log.d(TAG, "SSE Response Length: ${responseString.length} chars")
            val lines = responseString.split("\n")
            var currentEvent = ""
            var currentData = ""

            for (line in lines) {
                when {
                    line.startsWith("event:") -> {
                        currentEvent = line.substringAfter("event:").trim()
                    }
                    line.startsWith("data:") -> {
                        currentData = line.substringAfter("data:").trim()

                        // Parse the data to check for msg field (Gradio format)
                        try {
                            val jsonObject = org.json.JSONObject(currentData)
                            val msg = jsonObject.optString("msg", "")

                            if (msg == "process_completed") {
                                val success = jsonObject.optBoolean("success", false)
                                val output = jsonObject.optJSONObject("output")

                                Log.d(TAG, "SSE process_completed: success=$success, output=$output")

                                if (!success) {
                                    // Check if there's an error message in output
                                    val errorMsg = output?.optString("error") ?: "Unknown error"
                                    Log.e(TAG, "Process failed with error: $errorMsg")
                                    throw Exception("OOTDiffusion failed: ${if (errorMsg.isNullOrEmpty() || errorMsg == "null") "The model could not process the images. Please check that both images are valid and try again." else errorMsg}")
                                }

                                // Success - extract the result image from Gallery format
                                // OOTDiffusion returns: {"data": [[{"image": {"path": "..."}}]]}
                                // Note: Double nested array!
                                val dataArray = output?.optJSONArray("data")
                                if (dataArray != null && dataArray.length() > 0) {
                                    val firstElement = dataArray.get(0)

                                    // Check if it's a nested array (OOTDiffusion format)
                                    if (firstElement is org.json.JSONArray && firstElement.length() > 0) {
                                        val galleryItem = firstElement.getJSONObject(0)
                                        val imageObj = galleryItem.optJSONObject("image")
                                        if (imageObj != null) {
                                            val imagePath = imageObj.optString("path")
                                            if (imagePath.isNotEmpty()) {
                                                Log.d(TAG, "Found result image path (nested Gallery format): $imagePath")
                                                return imagePath
                                            }
                                        }
                                    }
                                    // Gallery format: {"image": {"path": "...", "url": "..."}}
                                    else if (firstElement is org.json.JSONObject) {
                                        val imageObj = firstElement.optJSONObject("image")
                                        if (imageObj != null) {
                                            val imageUrl = imageObj.optString("url") ?: imageObj.optString("path")
                                            if (imageUrl.isNotEmpty()) {
                                                Log.d(TAG, "Found result image URL (Gallery format): $imageUrl")
                                                return imageUrl
                                            }
                                        }

                                        // Fallback: direct path/url in first element
                                        val imageUrl = firstElement.optString("url") ?: firstElement.optString("path")
                                        if (imageUrl.isNotEmpty()) {
                                            Log.d(TAG, "Found result image URL (direct): $imageUrl")
                                            return imageUrl
                                        }
                                    } else if (firstElement is String) {
                                        Log.d(TAG, "Found result image URL (string): $firstElement")
                                        return firstElement
                                    }

                                    Log.e(TAG, "Could not extract image URL from: $firstElement")
                                } else {
                                    Log.e(TAG, "No output data in successful response")
                                    throw Exception("Server returned success but no result image")
                                }
                            }
                        } catch (e: org.json.JSONException) {
                            // Not JSON or different format, continue
                        }
                    }
                    line.isEmpty() && currentEvent.isNotEmpty() -> {
                        // Process the event (legacy format)
                        Log.d(TAG, "SSE Event: $currentEvent, Data: $currentData")

                        if (currentEvent == "complete") {
                            // Parse the JSON data
                            if (currentData == "null" || currentData.isEmpty()) {
                                Log.e(TAG, "Complete event with null data")
                                throw Exception("Server returned no result. The AI service may be unavailable.")
                            }

                            // Try to parse as Gradio v5 format first (single object or array with objects)
                            try {
                                // Check if it's a single FileData object
                                val jsonObject = org.json.JSONObject(currentData)
                                val imageUrl = jsonObject.optString("url") ?: jsonObject.optString("path")
                                if (imageUrl.isNotEmpty()) {
                                    Log.d(TAG, "Found image URL in SSE (v5 single object): $imageUrl")
                                    return imageUrl
                                }
                            } catch (e: org.json.JSONException) {
                                // Not a single object, try array format
                                try {
                                    val jsonData = org.json.JSONArray(currentData)
                                    if (jsonData.length() > 0) {
                                        // Check if first element is a FileData object
                                        val firstElement = jsonData.get(0)
                                        if (firstElement is org.json.JSONObject) {
                                            val imageUrl = firstElement.optString("url") ?: firstElement.optString("path")
                                            if (imageUrl.isNotEmpty()) {
                                                Log.d(TAG, "Found image URL in SSE (v5 array): $imageUrl")
                                                return imageUrl
                                            }
                                        } else if (firstElement is org.json.JSONArray) {
                                            // Legacy format: nested arrays
                                            val resultData = jsonData.getJSONArray(0)
                                            if (resultData.length() > 0) {
                                                val imageUrl = resultData.getString(0)
                                                Log.d(TAG, "Found image URL in SSE (legacy): $imageUrl")
                                                return imageUrl
                                            }
                                        }
                                    }
                                } catch (e2: org.json.JSONException) {
                                    Log.e(TAG, "Failed to parse as array either", e2)
                                }
                            }
                        } else if (currentEvent == "error") {
                            Log.e(TAG, "Error event received: $currentData")
                            // Log the full SSE response for debugging
                            Log.e(TAG, "Full SSE response when error occurred: $responseString")
                            val errorMsg = if (currentData == "null" || currentData.isEmpty()) {
                                "Server returned an error without details. Please check:\n" +
                                "• Your API endpoint is correctly configured\n" +
                                "• The server logs for more information\n" +
                                "• Try again in a moment"
                            } else {
                                currentData
                            }
                            throw Exception(errorMsg)
                        }

                        // Reset for next event
                        currentEvent = ""
                        currentData = ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SSE", e)
            throw e
        }

        return null
    }

    /**
     * Download image from URL
     */
    private fun downloadImageFromUrl(url: String): ByteArray {
        val fullUrl = if (url.startsWith("http")) {
            url
        } else {
            // Gradio file paths need to be accessed via /file= endpoint
            "${_apiUrl.value.removeSuffix("/")}/file=$url"
        }
        Log.d(TAG, "Downloading image from: $fullUrl")
        val response = java.net.URL(fullUrl).openStream()
        return response.readBytes()
    }

    /**
     * Reset all state
     */
    fun reset() {
        _bodyImageUri.value = null
        _clothingImageUri.value = null
        _tryOnResult.value = TryOnResult.Idle
    }

    /**
     * Reset only the result (keep images)
     */
    fun resetResult() {
        _tryOnResult.value = TryOnResult.Idle
    }
}
