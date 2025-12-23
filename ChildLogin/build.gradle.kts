// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Version information is centralized in gradle.properties
// Update agpVersion, kotlinVersion, and kspVersion there, then update the versions below to match
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("com.android.library") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false

}