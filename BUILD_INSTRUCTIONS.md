# Build Instructions

## Prerequisites Checklist

- [ ] Android Studio installed (Arctic Fox or later)
- [ ] JDK 8 or higher installed
- [ ] Android SDK API 34 installed
- [ ] Internet connection (for dependency downloads)

## First Time Setup

### 1. Install Android Studio

Download from: https://developer.android.com/studio

### 2. Install Required SDK Components

Open Android Studio → Tools → SDK Manager:
- [ ] Android SDK Platform 34
- [ ] Android SDK Build-Tools
- [ ] Android Emulator (if not using physical device)

### 3. Set JAVA_HOME (if needed)

**macOS/Linux:**
```bash
export JAVA_HOME=/path/to/jdk
export PATH=$JAVA_HOME/bin:$PATH
```

**Windows:**
```cmd
setx JAVA_HOME "C:\Program Files\Java\jdk-XX"
```

## Build Commands

### Option 1: Using Android Studio (Recommended for beginners)

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to this directory
4. Wait for Gradle sync (bottom status bar)
5. Click Run ▶️ button

### Option 2: Using Command Line

**macOS/Linux:**

```bash
# Navigate to project directory
cd "/Users/arezoughanekanafi/android apps/TryOn"

# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build and run
./gradlew installDebug
adb shell am start -n com.tryon.virtualfit/.MainActivity
```

**Windows:**

```cmd
# Navigate to project directory
cd "C:\path\to\TryOn"

# Build debug APK
gradlew.bat assembleDebug

# Install on connected device
gradlew.bat installDebug
```

### Build Outputs

After building, find your APK at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Common Build Issues

### Issue: "Gradle sync failed"

**Solution:**
```bash
# Clean project
./gradlew clean

# Invalidate caches in Android Studio
File → Invalidate Caches → Invalidate and Restart
```

### Issue: "SDK location not found"

**Solution:**
Create `local.properties` file in project root:
```properties
sdk.dir=/path/to/Android/Sdk
```

**Find SDK path:**
- Android Studio → Tools → SDK Manager → Android SDK Location

### Issue: "Cannot resolve symbol R"

**Solution:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### Issue: "Unsupported class file major version"

**Solution:**
- Update to Java 11 or higher
- Or change in `build.gradle.kts`:
```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
```

### Issue: Dependencies download failing

**Solution:**
- Check internet connection
- Try changing DNS to 8.8.8.8
- Check proxy settings in `gradle.properties`

## Building for Release

### Step 1: Create Signing Key

```bash
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release-key
```

Enter details when prompted and remember your passwords!

### Step 2: Configure Signing

Create `keystore.properties` in project root:
```properties
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=release-key
storeFile=../release-key.jks
```

Add to `.gitignore`:
```
keystore.properties
*.jks
```

### Step 3: Update build.gradle.kts

Add before `android {}` block in `app/build.gradle.kts`:

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            // ... existing config
        }
    }
}
```

### Step 4: Build Release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Step 5: (Optional) Build AAB for Play Store

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Testing

### Run Unit Tests

```bash
./gradlew test
```

### Run on Emulator

```bash
# List available emulators
emulator -list-avds

# Start emulator
emulator -avd Pixel_5_API_34

# Install and run
./gradlew installDebug
```

### Run on Physical Device

1. Enable Developer Options on device:
   - Settings → About Phone → Tap "Build Number" 7 times

2. Enable USB Debugging:
   - Settings → Developer Options → USB Debugging

3. Connect device via USB

4. Verify connection:
   ```bash
   adb devices
   ```

5. Install:
   ```bash
   ./gradlew installDebug
   ```

## Performance Optimization

### Reduce APK Size

In `app/build.gradle.kts`:

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### Enable R8 Full Mode

In `gradle.properties`:
```properties
android.enableR8.fullMode=true
```

### Analyze APK

```bash
./gradlew assembleRelease
```

Then in Android Studio:
Build → Analyze APK → Select the APK

## Continuous Integration

### GitHub Actions Example

Create `.github/workflows/android.yml`:

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    - name: Build with Gradle
      run: ./gradlew build
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

## Gradle Properties Reference

Useful properties for `gradle.properties`:

```properties
# Performance
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
org.gradle.parallel=true
org.gradle.caching=true

# AndroidX
android.useAndroidX=true
android.enableJetifier=true

# Kotlin
kotlin.code.style=official

# R8
android.enableR8=true
android.enableR8.fullMode=true
```

## Troubleshooting Build Performance

### Slow Gradle builds?

1. Enable Gradle daemon:
   ```properties
   # In gradle.properties
   org.gradle.daemon=true
   ```

2. Increase memory:
   ```properties
   org.gradle.jvmargs=-Xmx4096m
   ```

3. Enable parallel execution:
   ```properties
   org.gradle.parallel=true
   ```

4. Use build cache:
   ```properties
   org.gradle.caching=true
   ```

### Clean build when needed

```bash
./gradlew clean build --refresh-dependencies
```

## Version Management

Current versions in project:
- Gradle: 8.2
- Android Gradle Plugin: 8.2.0
- Kotlin: 1.9.20
- Compose: 1.5.4
- compileSdk: 34
- targetSdk: 34
- minSdk: 24

To update dependencies:
1. Check for updates in Android Studio: Tools → Dependencies
2. Update versions in `build.gradle.kts`
3. Sync and test

## Additional Resources

- [Android Build Guide](https://developer.android.com/studio/build)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Sign Your App](https://developer.android.com/studio/publish/app-signing)
- [ProGuard Rules](https://www.guardsquare.com/manual/configuration/usage)

---

Good luck with your build! 🚀
