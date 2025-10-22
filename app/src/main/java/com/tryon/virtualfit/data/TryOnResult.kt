package com.tryon.virtualfit.data

/**
 * Sealed class representing the result of a try-on operation
 */
sealed class TryOnResult {
    object Idle : TryOnResult()
    object Loading : TryOnResult()
    data class Success(val imageData: ByteArray) : TryOnResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            return imageData.contentEquals(other.imageData)
        }

        override fun hashCode(): Int {
            return imageData.contentHashCode()
        }
    }
    data class Error(val message: String) : TryOnResult()
}
