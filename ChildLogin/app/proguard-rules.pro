plugins {oject specific ProGuard rules here.
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")#############################################################
    id("com.google.dagger.hilt.android")रॉइड नियम #####################################
    kotlin("kapt")#####################################################################
-keepattributes Signature, *Annotation*
}keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
android {lic class * extends android.app.Service
    namespace = "com.safe.setting.app"ontent.BroadcastReceiver
    compileSdk = 36* extends android.content.ContentProvider
-keep public class * extends android.view.View
    defaultConfig {
        applicationId = "com.safe.setting.app"
        minSdk = 23>;
        targetSdk = 36
        versionCode = 8* {
        versionName = "8.80008";
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }classmembers class * implements android.os.Parcelable {
  public static final ** CREATOR;
    buildFeatures {
        viewBinding = true
        buildConfig = true#############################################################
    }######################### ऐप-विशिष्ट नियम ########################################
# #####################################################################################
    buildTypes {safe.setting.app.data.model.** { *; }
        release {afe.setting.app.ui.** { *; }
            isMinifyEnabled = trueervices.** { *; }
            isShrinkResources = trueeiver.** { *; }
            proguardFiles(ng.app.di.** { *; }
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"ls.** { *; }
            )
            signingConfig = signingConfigs.getByName("debug")##########################
        }#################### लाइब्रेरी-विशिष्ट नियम ##################################
    }##################################################################################

    compileOptions {s Library (daimajia)
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
## SweetAlert (क्रैश को ठीक करने के लिए)
    externalNativeBuild {eetAlert.** { *; }
        cmake {edant.SweetAlert.**
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        } dagger.internal.codegen.**
    }
# RxJava 3
    // 'packagingOptions' has been renamed to 'packaging' in newer AGP versions.
    packaging {
        // RxJava 3 doesn't need this exclusion anymore, but keeping it in case of other conflicts.
        resources.excludes.add("META-INF/rxjava.properties")
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}") अब उस
    }ब्रेरी का उपयोग नहीं कर रहे हैं।
# ===============================================================
    lint {ass com.cloudinary.** { *; }
        abortOnError = falsenary.** { *; }
    }ntwarn com.cloudinary.**
}
# ===============================================================
// Set the JVM toolchain and compiler options for Kotlin
kotlin { Supabase और उसकी निर्भरता (Ktor, Kotlinx Serialization) के लिए
    jvmToolchain(17)ोड़े गए हैं। यह सुनिश्चित करता है कि जब ऐप को
    compilerOptions {जाता है, तो इन लाइब्रेरीज़ का महत्वपूर्ण कोड न हटे,
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    } class io.supabase.** { *; }
}keep interface io.supabase.** { *; }
-dontwarn io.supabase.**
// Removed deprecated kotlinOptions task configuration; using compilerOptions above
-keep class kotlinx.serialization.** { *; }
dependencies {e kotlinx.serialization.** { *; }
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.cardview:cardview:1.0.0")
    // Android Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")

    // WorkManager KTX (resolves OneTimeWorkRequestBuilder, workDataOf, CoroutineWorker)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    @com.google.firebase.database.PropertyName *;
    // Kotlin Coroutines Android (for CoroutineWorker)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation("com.google.firebase:firebase-database:22.0.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.firebase:firebase-analytics:23.0.0")
//    val supabaseVersion = "3.0.0" // गाइड के  संस्करण 3.0.0    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.6"))    implementation("io.github.jan-tennert.supabase:postgrest-kt")    implementation("io.github.jan-tennert.supabase:storage-kt")    implementation("io.github.jan-tennert.supabase:auth-kt") // got rue-kt की जगह auth-kt    implementation("io.github.jan-tennert.supabase:realtime-kt") // Realtime के लिए जोड़ा गया    implementation("io.ktor:ktor-client-okhttp:3.3.2")    // लॉजिक: minSdk 26 से कम होने पर Java 8+ APIs को सपोर्ट    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")    // Glide Image Loading (Latest)    implementation("com.github.bumptech.glide:glide:5.0.5")    kapt("com.github.bumptech.glide:compiler:5.0.5")    // Hilt (Updated to 2.57.2 & Switched to KSP)    val hiltVersion = "2.57.2"    implementation("com.google.dagger:hilt-android:$hiltVersion")    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")    // Hilt Worker integration (Updated to 1.2.0)    implementation("androidx.hilt:hilt-work:1.2.0")    kapt("androidx.hilt:hilt-compiler:1.2.0")    // RxJava3 & RxBinding    implementation("com.jakewharton.rxbinding4:rxbinding:4.0.0")    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")    // Testing    testImplementation("junit:junit:4.13.2")    androidTestImplementation("androidx.test.ext:junit:1.3.0")    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")}//Universal Developers Private Limitedconst val KEY_FILE_PATH = "FILE_PATH"const val KEY_FILE_TYPE = "FILE_TYPE"const val KEY_RANDOM_NAME = "RANDOM_NAME"
const val KEY_ERROR = "error"

val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
val fileType = inputData.getString(KEY_FILE_TYPE) ?: return Result.failure()
val randomName = inputData.getString(KEY_RANDOM_NAME) ?: return Result.failure(Data.Builder().putString(KEY_ERROR, "Missing RANDOM_NAME").build())

if (!file.exists() || file.length() == 0L) {
    updateStatus(fileType, randomName, Consts.STATE_FAILED, "File not found or empty")
    return Result.failure(