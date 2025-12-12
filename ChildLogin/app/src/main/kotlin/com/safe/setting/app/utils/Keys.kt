package com.safe.setting.app.utils

object Keys {

    // यह सिस्टम को बताता है कि "native-lib" नाम की लाइब्रेरी को लोड करना है।
    // यह नाम CMakeLists.txt में दिए गए नाम से मेल खाना चाहिए।
    init {
        System.loadLibrary("native-lib")
    }

    // ===============================================================
    // Firebase Keys (आपका पुराना कोड)
    // ===============================================================
    external fun getFirebaseDatabaseUrl(): String
    external fun getGcmDefaultSenderId(): String
    external fun getGoogleApiKey(): String
    external fun getGoogleAppId(): String
    external fun getProjectId(): String

    // ===============================================================
    // नया Supabase कोड
    // लॉजिक: Cloudinary के फंक्शन्स को हटाकर Supabase के लिए नए external
    // फंक्शन्स घोषित किए गए हैं। यह कोटलिन कोड को C++ में संग्रहीत
    // सुरक्षित Keys तक पहुंचने की अनुमति देता है।
    // ===============================================================
    external fun getSupabaseUrl(): String
    external fun getSupabaseAnonKey(): String
}
