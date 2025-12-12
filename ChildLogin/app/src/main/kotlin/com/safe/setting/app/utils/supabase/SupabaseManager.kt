package com.safe.setting.app.utils.supabase

import android.util.Log
import com.safe.setting.app.utils.Consts
import com.safe.setting.app.utils.Keys
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import io.github.jan.supabase.auth.Auth

/**
 * लॉजिक: यह एक Singleton ऑब्जेक्ट है जो Supabase के साथ सभी इंटरैक्शन को मैनेज करता है।
 * इसे एक ही स्थान पर रखने से कोड साफ रहता है और इसे मैनेज करना आसान होता है।
 * यह फाइल अपलोड करने और उनका पब्लिक URL प्राप्त करने के लिए जिम्मेदार है।
 */
object SupabaseManager {

    private lateinit var supabaseClient: SupabaseClient

    /**
     * हिंदी कमेंट: यह फ़ंक्शन Supabase क्लाइंट को इनिशियलाइज़ (सेटअप) करता है।
     * यह ऐप के शुरू होते ही 'Hom.kt' से एक बार कॉल किया जाना चाहिए।
     * '::supabaseClient.isInitialized' जांचता है कि क्लाइंट पहले से बना है या नहीं।
     */
    fun initialize() {
        if (!::supabaseClient.isInitialized) {
            supabaseClient = createSupabaseClient(
                supabaseUrl = Keys.getSupabaseUrl(),
                supabaseKey = Keys.getSupabaseAnonKey()
            ) {
                install(Auth)
                install(Storage)
                install(Realtime)
            }
        }
    }

    /**
     * हिंदी कमेंट: यह फ़ंक्शन एक फ़ाइल (फोटो, वीडियो, ऑडियो) को Supabase स्टोरेज पर अपलोड करता है।
     * यह एक suspend फ़ंक्शन है, जिसका मतलब है कि यह बैकग्राउंड थ्रेड पर चलना चाहिए ताकि UI फ्रीज न हो।
     * @param file अपलोड की जाने वाली फ़ाइल।
     * @param bucketName स्टोरेज बकेट का नाम (जैसे "media_files")।
     * @return सफल होने पर फ़ाइल का सार्वजनिक URL लौटाता है।
     * @throws Exception यदि अपलोड विफल होता है।
     */
    suspend fun uploadFile(file: File, bucketName: String = "uploads"): String {
        if (!::supabaseClient.isInitialized) {
            initialize()
        }

        return withContext(Dispatchers.IO) {
            try {
                val storage = supabaseClient.storage
                val filePath = "uploads/${System.currentTimeMillis()}-${file.name}"
                val fileBytes = file.readBytes()

                // --- मुख्य बदलाव यहाँ है ---
                // लॉजिक: Supabase V3 में, 'upsert' जैसे विकल्प अब एक अलग 'options' ब्लॉक के अंदर दिए जाते हैं।
                // यह लाइब्रेरी को अधिक लचीला और भविष्य के लिए तैयार बनाता है।
                storage.from(bucketName).upload(path = filePath, data = fileBytes) {
                    upsert = true
                }
                // --- बदलाव समाप्त ---

                val publicUrl = storage.from(bucketName).publicUrl(filePath)
                Log.d(Consts.TAG, "Supabase Upload Success: $publicUrl")
                publicUrl
            } catch (e: Exception) {
                Log.e(Consts.TAG, "Supabase Upload Error: ${e.message}")
                throw e
            }
        }
    }
}

