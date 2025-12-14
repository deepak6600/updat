package com.safe.setting.app.utils

import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.GZIPOutputStream

object GzipUtils {
    private const val TAG = "GzipUtils"

    fun compressToBase64(input: String, charset: Charset = Charsets.UTF_8): String {
        try {
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).use { gzip ->
                val bytes = input.toByteArray(charset)
                gzip.write(bytes)
            }
            val compressed = bos.toByteArray()
            val base64 = Base64.encodeToString(compressed, Base64.NO_WRAP)
            Log.d(TAG, "Compressed ${input.length} chars to ${base64.length} base64 chars")
            return base64
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed: ${e.message}")
            return input // Fallback to raw if compression fails
        }
    }
}
