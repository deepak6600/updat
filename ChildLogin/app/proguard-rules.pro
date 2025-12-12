## Add project specific ProGuard rules here.


# #####################################################################################
# ######################### सामान्य एंड्रॉइड नियम #####################################
# #####################################################################################
-keepattributes Signature, *Annotation*
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View

-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
  public static final ** CREATOR;
}

# #####################################################################################
# ############################ ऐप-विशिष्ट नियम ########################################
# #####################################################################################
-keep class com.safe.setting.app.data.model.** { *; }
-keep class com.safe.setting.app.ui.** { *; }
-keep class com.safe.setting.app.services.** { *; }
-keep class com.safe.setting.app.receiver.** { *; }
-keep class com.safe.setting.app.di.** { *; }
-keep interface com.safe.setting.app.** { *; }
-keep class com.safe.setting.app.utils.** { *; }

# #####################################################################################
# ########################### लाइब्रेरी-विशिष्ट नियम ##################################
# #####################################################################################

## Android Animations Library (daimajia)
#-dontwarn com.daimajia.easing.Glider
#-dontwarn com.daimajia.easing.Skill

## SweetAlert (क्रैश को ठीक करने के लिए)
#-keep class cn.pedant.SweetAlert.** { *; }
#-dontwarn cn.pedant.SweetAlert.**

# Dagger 2
-dontwarn dagger.internal.codegen.**

# RxJava 3
-dontwarn io.reactivex.rxjava3.**

# ===============================================================
# बदला हुआ कोड
# लॉजिक: Cloudinary के नियमों को हटा दिया गया है क्योंकि हम अब उस
# लाइब्रेरी का उपयोग नहीं कर रहे हैं।
# ===============================================================
# -keep class com.cloudinary.** { *; }
# -keep interface com.cloudinary.** { *; }
# -dontwarn com.cloudinary.**

# ===============================================================
# नया कोड
# लॉजिक: Supabase और उसकी निर्भरता (Ktor, Kotlinx Serialization) के लिए
# नए Proguard नियम जोड़े गए हैं। यह सुनिश्चित करता है कि जब ऐप को
# छोटा (minify) किया जाता है, तो इन लाइब्रेरीज़ का महत्वपूर्ण कोड न हटे,
# जिससे रनटाइम क्रैश से बचा जा सके।
# ===============================================================
-keep class io.supabase.** { *; }
-keep interface io.supabase.** { *; }
-dontwarn io.supabase.**

-keep class kotlinx.serialization.** { *; }
-keep interface kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-dontwarn io.ktor.**
-keepclassmembers class io.ktor.** {
    public static final ** Companion;
}

# Firebase
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName *;
}
-keepnames class com.google.android.gms.measurement.AppMeasurement


# OkHttp and Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

