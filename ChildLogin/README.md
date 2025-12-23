# ChildLogin Android Application

## Overview
ChildLogin is an Android application for child monitoring and parental control. This application is distributed via website downloads and not through the Google Play Store.

## Build Requirements

### Required SDK
- **Android SDK**: API Level 34 (Android 14)
- **Minimum SDK**: API Level 23 (Android 6.0 Marshmallow)
- **Target SDK**: API Level 34 (Android 14)
- **Java**: JDK 11 or higher
- **Gradle**: 8.5 (using Gradle wrapper)

### Android Studio
- Android Studio Hedgehog (2023.1.1) or newer
- Compatible with Android Studio 10/11 and future versions

## Building from Source

### Setup
1. Clone the repository
2. Ensure Android SDK is installed with API Level 34
3. Create a `local.properties` file in the `ChildLogin` directory with your SDK location:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ```
4. **Network Requirements**: Building requires internet access to:
   - Google Maven Repository (`dl.google.com`) for Android Gradle Plugin and AndroidX libraries
   - Maven Central (`repo1.maven.org`) for general dependencies
   - Gradle Plugin Portal for build tools

### Build Commands

#### Clean Build
```bash
./gradlew clean
```

#### Debug Build
```bash
./gradlew assembleDebug
```

#### Release Build
```bash
./gradlew assembleRelease
```

#### Run Lint Checks
```bash
./gradlew lint
```

#### Run Tests
```bash
./gradlew test
```

### APK Output Location
After a successful build, the APK files can be found at:
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

## Version Configuration

All version information is centralized in `gradle.properties`:
- `androidCompileSdk`: Compile SDK version
- `androidTargetSdk`: Target SDK version
- `androidMinSdk`: Minimum SDK version
- `agpVersion`: Android Gradle Plugin version
- `kotlinVersion`: Kotlin language version
- `gradleWrapperVersion`: Gradle wrapper version

To update SDK versions for future Android releases, simply modify these properties in `gradle.properties`.

## Project Structure
```
ChildLogin/
├── app/                          # Main application module
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/           # Kotlin source files
│   │       ├── res/              # Resources (layouts, drawables, etc.)
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts          # App-level build configuration
│   └── proguard-rules.pro        # ProGuard/R8 rules
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Centralized version configuration
└── gradlew                       # Gradle wrapper script
```

## Dependencies
The project uses modern AndroidX libraries and includes:
- Material Design Components
- Firebase Authentication and Database
- Supabase for backend services
- WorkManager for background tasks
- Hilt for dependency injection
- RxJava3 for reactive programming
- Glide for image loading

## Troubleshooting

### Build Fails with "SDK not found"
Ensure `local.properties` exists with the correct `sdk.dir` path.

### Gradle Sync Issues
Try running:
```bash
./gradlew clean build --refresh-dependencies
```

### Permission Issues on Linux/Mac
Make the Gradle wrapper executable:
```bash
chmod +x gradlew
```

## Security Notes
- Never commit `local.properties`, `*.jks`, `*.keystore`, or `keystore.properties` files to version control
- These files are automatically excluded via `.gitignore`

## Adaptive Icons
The application includes adaptive icons for modern Android launchers (API 26+):
- Location: `app/src/main/res/mipmap-anydpi-v26/`
- Files: `c_launcher.xml`, `c_launcher_round.xml`
- Configuration: Uses separate background and foreground layers for dynamic launcher presentation

## License
Proprietary - Universal Developers Private Limited

## Support
For build issues or questions, please refer to the project documentation or contact the development team.
