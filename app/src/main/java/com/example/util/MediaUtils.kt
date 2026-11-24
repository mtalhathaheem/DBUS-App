package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.model.MediaType
import java.io.ByteArrayOutputStream
import java.util.Locale

object MediaUtils {

    // Size and duration limits (Requirement 4)
    const val MAX_VIDEO_DURATION_SECONDS = 120 // 2 minutes
    const val MAX_VIDEO_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB
    const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB raw
    const val MAX_AUDIO_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
    const val MAX_DOCUMENT_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB

    /**
     * Resizes and compresses a Bitmap to a compact JPEG Base64 string (Requirement 4: Make photos smaller before upload).
     * Downscales to maxDimension (e.g. 800px) and compresses to ~80% JPEG quality, typically yielding < 150KB.
     */
    fun compressBitmapToBase64(bitmap: Bitmap, maxDimension: Int = 800, quality: Int = 80): String {
        val width = bitmap.width
        val height = bitmap.height

        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val (newWidth, newHeight) = if (ratio > 1f) {
                maxDimension to (maxDimension / ratio).toInt()
            } else {
                (maxDimension * ratio).toInt() to maxDimension
            }
            Bitmap.createScaledBitmap(bitmap, newWidth.coerceAtLeast(1), newHeight.coerceAtLeast(1), true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    /**
     * Decodes Base64 data string back to Android Bitmap.
     */
    fun decodeBase64ToBitmap(dataUri: String): Bitmap? {
        return try {
            val base64Data = if (dataUri.contains(",")) {
                dataUri.substringAfter(",")
            } else {
                dataUri
            }
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats bytes into human-readable size (KB / MB).
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }

    /**
     * Formats seconds into mm:ss format.
     */
    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    /**
     * Validates media upload against limits (Requirement 4).
     * Returns Pair(isValid, errorMessage).
     */
    fun validateMediaUpload(
        mediaType: MediaType,
        fileSizeBytes: Long,
        durationSeconds: Int = 0,
        isUrdu: Boolean = false
    ): Pair<Boolean, String?> {
        when (mediaType) {
            MediaType.VIDEO -> {
                if (durationSeconds > MAX_VIDEO_DURATION_SECONDS) {
                    val msg = if (isUrdu) {
                        "ویڈیو کا دورانیہ 2 منٹ (120 سیکنڈ) سے کم ہونا ضروری ہے۔ موجودہ دورانیہ: ${durationSeconds} سیکنڈ"
                    } else {
                        "Video exceeds 2 minutes limit (duration is ${durationSeconds}s, maximum allowed is 120s)."
                    }
                    return false to msg
                }
                if (fileSizeBytes > MAX_VIDEO_SIZE_BYTES) {
                    val msg = if (isUrdu) {
                        "ویڈیو فائل 50 ایم بی کی حد سے تجاوز کر گئی ہے۔ موجودہ سائز: ${formatFileSize(fileSizeBytes)}"
                    } else {
                        "Video file exceeds 50 MB limit (current size: ${formatFileSize(fileSizeBytes)}, maximum allowed is 50 MB)."
                    }
                    return false to msg
                }
            }
            MediaType.PHOTO -> {
                if (fileSizeBytes > MAX_IMAGE_SIZE_BYTES) {
                    val msg = if (isUrdu) {
                        "تصویر فائل کا سائز 10 ایم بی سے کم ہونا ضروری ہے۔ موجودہ سائز: ${formatFileSize(fileSizeBytes)}"
                    } else {
                        "Photo file size exceeds 10 MB limit (current size: ${formatFileSize(fileSizeBytes)})."
                    }
                    return false to msg
                }
            }
            MediaType.AUDIO -> {
                if (fileSizeBytes > MAX_AUDIO_SIZE_BYTES) {
                    val msg = if (isUrdu) {
                        "آڈیو فائل کا سائز 25 ایم بی سے کم ہونا ضروری ہے۔ موجودہ سائز: ${formatFileSize(fileSizeBytes)}"
                    } else {
                        "Audio file exceeds 25 MB limit (current size: ${formatFileSize(fileSizeBytes)})."
                    }
                    return false to msg
                }
            }
            MediaType.DOCUMENT -> {
                if (fileSizeBytes > MAX_DOCUMENT_SIZE_BYTES) {
                    val msg = if (isUrdu) {
                        "دستاویز کا سائز 25 ایم بی سے کم ہونا ضروری ہے۔ موجودہ سائز: ${formatFileSize(fileSizeBytes)}"
                    } else {
                        "Document exceeds 25 MB limit (current size: ${formatFileSize(fileSizeBytes)})."
                    }
                    return false to msg
                }
            }
        }
        return true to null
    }
}
