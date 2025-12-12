package com.safe.setting.app.di.module

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.safe.setting.app.data.rxFirebase.DevelopFirebase
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import com.safe.setting.app.utils.Keys
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseApp(@ApplicationContext context: Context): FirebaseApp {
        val options = FirebaseOptions.Builder()
            .setApiKey(Keys.getGoogleApiKey())
            .setApplicationId(Keys.getGoogleAppId())
            .setDatabaseUrl(Keys.getFirebaseDatabaseUrl())
            // .setStorageBucket(...) को हटा दिया गया है क्योंकि इसकी अब ज़रूरत नहीं है
            .setGcmSenderId(Keys.getGcmDefaultSenderId())
            .setProjectId(Keys.getProjectId())
            .build()

        return FirebaseApp.getApps(context).find { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(context, options)
    }

    // StorageReference को इस फंक्शन से हटा दिया गया है
    @Provides
    fun provideInterfaceFirebase(@ApplicationContext context: Context, auth: FirebaseAuth, dataRef: DatabaseReference): InterfaceFirebase =
        DevelopFirebase(context, auth, dataRef)

    @Provides
    fun provideFirebaseAuth(app: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(app)

    @Provides
    fun provideFirebaseDatabase(app: FirebaseApp): FirebaseDatabase = FirebaseDatabase.getInstance(app)

    @Provides
    fun provideDatabaseReference(database: FirebaseDatabase): DatabaseReference = database.reference

    // FirebaseStorage और StorageReference से जुड़े सभी @Provides फंक्शन्स हटा दिए गए हैं
    // fun provideFirebaseStorage(...)
    // fun provideStorageReference(...)
}
