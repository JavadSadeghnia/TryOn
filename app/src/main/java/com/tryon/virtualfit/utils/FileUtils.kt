package com.tryon.virtualfit.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utility class for file operations
 */
object FileUtils {

    /**
     * Convert a URI to a File
     * @param context Application context
     * @param uri The URI to convert
     * @return File object or null if conversion fails
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            android.util.Log.d("FileUtils", "Converting URI to File: $uri, scheme=${uri.scheme}")

            // If it's a file URI pointing to cache dir, return it directly
            if (uri.scheme == "content" && uri.authority?.contains("fileprovider") == true) {
                val path = uri.path
                android.util.Log.d("FileUtils", "FileProvider URI detected, path: $path")

                // Try to extract the actual file path
                path?.let {
                    // FileProvider URIs typically have format: content://authority/cache-path/filename
                    val file = File(context.cacheDir, it.substringAfterLast("/"))
                    if (file.exists()) {
                        android.util.Log.d("FileUtils", "Direct file access successful: ${file.absolutePath}, size=${file.length()}")
                        return file
                    } else {
                        android.util.Log.w("FileUtils", "Direct file doesn't exist: ${file.absolutePath}, falling back to stream copy")
                    }
                }
            }

            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.let {
                android.util.Log.d("FileUtils", "InputStream opened successfully")

                // Create a temporary file
                val fileName = getFileName(context, uri) ?: "temp_image_${System.currentTimeMillis()}.jpg"
                val tempFile = File(context.cacheDir, fileName)

                var totalBytes = 0
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }
                }
                inputStream.close()

                android.util.Log.d("FileUtils", "File created: ${tempFile.absolutePath}, size=${tempFile.length()}, copied=$totalBytes bytes")
                tempFile
            }
        } catch (e: Exception) {
            android.util.Log.e("FileUtils", "Error converting URI to file", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Get the file name from a URI
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null

        // Try to get the file name from the URI
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        }

        // Fallback to the last path segment
        if (fileName == null) {
            fileName = uri.lastPathSegment
        }

        return fileName
    }

    /**
     * Create a temporary image file for camera capture
     */
    fun createImageFile(context: Context): File {
        val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
        return File(context.cacheDir, fileName)
    }
}
