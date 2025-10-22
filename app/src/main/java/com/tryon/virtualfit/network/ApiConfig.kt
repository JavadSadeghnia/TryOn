package com.tryon.virtualfit.network

/**
 * API Configuration
 * Central place to manage API URLs
 */
object ApiConfig {
    /**
     * Default API URL - This should point to your permanent backend
     *
     * OPTIONS:
     * 1. Keep Colab running 24/7 (not practical)
     * 2. Use HuggingFace Spaces (free, permanent, but has rate limits)
     * 3. Deploy to cloud (costs money but reliable)
     * 4. Use a fallback URL service
     */

    // Primary API URL - Update this with your permanent backend
    const val DEFAULT_API_URL = "https://38845346fa95d8fa88.gradio.live/"

    // Fallback API URLs in case primary fails
    val FALLBACK_URLS = listOf(
        "https://fallback1.gradio.live",
        "https://fallback2.gradio.live"
    )

    /**
     * Check if app should show API configuration UI to users
     * Set to false for production to hide settings from regular users
     */
    const val ALLOW_USER_API_CONFIG = true  // show settings icon to change URL

    /**
     * Feature flags
     */
    const val ENABLE_API_FALLBACK = true
    const val ENABLE_RETRY_LOGIC = true
}
