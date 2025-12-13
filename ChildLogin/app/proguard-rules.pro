############################ ProGuard / R8 rules ############################

-keepattributes *Annotation*, Signature

-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Hilt/Dagger
-keep class dagger.hilt.internal.** { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class **_HiltModules { *; }
-dontwarn dagger.hilt.internal.**
-dontwarn javax.inject.**

# AndroidX Hilt
-keep class androidx.hilt.** { *; }
-dontwarn androidx.hilt.**

# Glide
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# Kotlin/Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.**

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Play Services Location internals
-dontwarn com.google.android.gms.internal.location.**

# Supabase & Ktor
-keep class io.supabase.** { *; }
-keep interface io.supabase.** { *; }
-dontwarn io.supabase.**
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# OkHttp/Okio
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# RxJava
# Removed Rx dontwarn; keep meaningful warnings

# App package keeps (models, ui, services, receiver, di, utils)
-keep class com.safe.setting.app.data.model.** { *; }
-keep class com.safe.setting.app.ui.** { *; }
-keep class com.safe.setting.app.services.** { *; }
-keep class com.safe.setting.app.receiver.** { *; }
-keep class com.safe.setting.app.di.** { *; }
-keep class com.safe.setting.app.utils.** { *; }

-dontnote **