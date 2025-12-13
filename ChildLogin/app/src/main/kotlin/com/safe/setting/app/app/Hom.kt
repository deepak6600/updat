package com.safe.setting.app.app
import android.app.Application
import androidx.work.Configuration
import androidx.multidex.MultiDex
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.safe.setting.app.utils.Consts.SIZE_CACHE_FIREBASE
import com.safe.setting.app.utils.supabase.SupabaseManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

@HiltAndroidApp
class Hom : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)

        // ===============================================================
        // बदला हुआ कोड
        // लॉजिक: CloudinaryManager.initialize(this) को हटा दिया गया है
        // और उसकी जगह SupabaseManager.initialize() को कॉल किया गया है।
        // यह सुनिश्चित करता है कि ऐप शुरू होते ही Cloudinary की जगह
        // Supabase से कनेक्ट हो।
        // ===============================================================
        SupabaseManager.initialize()

        if (FirebaseApp.getApps(this).isNotEmpty()) {
            val database = FirebaseDatabase.getInstance()
            database.setPersistenceEnabled(true)
            database.setPersistenceCacheSizeBytes(SIZE_CACHE_FIREBASE)
        }

    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}

