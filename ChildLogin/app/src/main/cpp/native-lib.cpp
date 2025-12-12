#include <jni.h>
#include <string>

// यह एक हेल्पर फ़ंक्शन है जो C++ स्ट्रिंग से जावा स्ट्रिंग बनाता है
jstring createJString(JNIEnv* env, const char* str) {
    return env->NewStringUTF(str);
}

// extern "C" यह सुनिश्चित करता है कि C++ कंपाइलर नामों को न बदले
extern "C" {

// ===============================================================
// Firebase Keys (आपका पुराना कोड)
// ===============================================================
JNIEXPORT jstring JNICALL
Java_com_safe_setting_app_utils_Keys_getFirebaseDatabaseUrl(JNIEnv *env, jobject thiz) {
//    return createJString(env, "https://home-demo12-d5814-default-rtdb.firebaseio.com");
    return createJString(env, "https://famtoolapp-23028-default-rtdb.asia-southeast1.firebasedatabase.app");
}

JNIEXPORT jstring JNICALL
Java_com_safe_setting_app_utils_Keys_getGcmDefaultSenderId(JNIEnv *env, jobject thiz) {
//    return createJString(env, "433464727867");
    return createJString(env, "823040432046");
}

JNIEXPORT jstring JNICALL
Java_com_safe_setting_app_utils_Keys_getGoogleApiKey(JNIEnv *env, jobject thiz) {
//    return createJString(env, "AIzaSyBArvJ6KEuljzWvUKGSNcbp1dLmJWWyz6o");
    return createJString(env, "AIzaSyCcpgSbizPnAIfza97vwVnjZAQtoug5FuU");
}

JNIEXPORT jstring JNICALL
Java_com_safe_setting_app_utils_Keys_getGoogleAppId(JNIEnv *env, jobject thiz) {
    return createJString(env, "1:823040432046:android:38564ee2de23f9693f1a3f");
}

JNIEXPORT jstring JNICALL
Java_com_safe_setting_app_utils_Keys_getProjectId(JNIEnv *env, jobject thiz) {
    return createJString(env, "famtoolapp-23028");
}

// ===============================================================
// नया Supabase कोड
// लॉजिक: Cloudinary से संबंधित सभी फंक्शन्स को हटा दिया गया है
// और उनकी जगह Supabase के लिए नए फंक्शन्स जोड़े गए हैं।
// यह आपके संवेदनशील (sensitive) Keys को सुरक्षित रखता है।
// ===============================================================

JNIEXPORT jstring JNICALL
Java_com_safe_setting_app_utils_Keys_getSupabaseUrl(JNIEnv *env, jobject thiz) {
    // यहाँ आपकी Supabase प्रोजेक्ट का URL है।
    return createJString(env, "https://wbjnsxlgnapnziwgychh.supabase.co");
}

JNIEXPORT jstring JNICALL
Java_com_safe_setting_app_utils_Keys_getSupabaseAnonKey(JNIEnv *env, jobject thiz) {
    // यह आपकी Supabase की पब्लिक 'anon' की है, जो क्लाइंट-साइड उपयोग के लिए सुरक्षित है।
    return createJString(env, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Indiam5zeGxnbmFwbnppd2d5Y2hoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1ODcwMzMsImV4cCI6MjA3MzE2MzAzM30.-ecfnLjzOSAwn1IyjUKuAF1uLLSaNRcy9fBstnIyFxw");
}


} // extern "C" ब्लॉक का अंत
