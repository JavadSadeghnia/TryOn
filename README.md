# Virtual Try-On App

A free AI-powered virtual try-on application built with Android (Kotlin + Jetpack Compose) and Google Colab for AI processing.

## Features

- Upload or capture body photos
- Upload clothing images
- AI-powered virtual try-on generation
- Real-time preview of results
- Free infrastructure using Google Colab

## Project Structure

```
TryOn/
├── app/
│   ├── src/main/
│   │   ├── java/com/tryon/virtualfit/
│   │   │   ├── data/
│   │   │   │   └── TryOnResult.kt          # Result data model
│   │   │   ├── network/
│   │   │   │   ├── TryOnApiService.kt      # API interface
│   │   │   │   └── RetrofitClient.kt       # HTTP client
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   └── HomeScreen.kt       # Main UI
│   │   │   │   └── theme/                  # App theming
│   │   │   ├── utils/
│   │   │   │   └── FileUtils.kt            # File operations
│   │   │   ├── viewmodel/
│   │   │   │   └── TryOnViewModel.kt       # State management
│   │   │   └── MainActivity.kt             # Entry point
│   │   ├── res/                            # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── VirtualTryOn_Colab.ipynb                # AI Backend
└── README.md
```

## Prerequisites

### For Android Development

- Android Studio (latest version)
- JDK 8 or higher
- Android SDK API 24+ (Android 7.0+)
- Physical Android device or emulator

### For AI Backend

- Google Account (for Colab)
- Internet connection

## Setup Instructions

### Part 1: Set Up AI Backend (Google Colab)

1. **Open Google Colab**
   - Go to [Google Colab](https://colab.research.google.com/)
   - Sign in with your Google account

2. **Upload the Notebook**
   - Upload `VirtualTryOn_Colab.ipynb` to Colab
   - Or open it from GitHub/Google Drive

3. **Enable GPU**
   - Click `Runtime` → `Change runtime type`
   - Set `Hardware accelerator` to `GPU`
   - Click `Save`

4. **Run the Notebook**
   - Run each cell in order (click the play button or press `Shift+Enter`)
   - Wait for dependencies to install (2-3 minutes)

5. **Get Your API URL**
   - After running the last cell, you'll see output like:
     ```
     Running on public URL: https://xxxxx.gradio.live
     ```
   - **COPY THIS URL** - you'll need it for the Android app!
   - The URL is valid for the duration of your Colab session

### Part 2: Build and Run Android App

1. **Open Project in Android Studio**
   ```bash
   cd "TryOn"
   # Open Android Studio and select "Open an Existing Project"
   # Navigate to this directory
   ```

2. **Sync Gradle**
   - Android Studio will automatically sync Gradle
   - Wait for dependencies to download (first time may take 5-10 minutes)
   - If sync fails, try `File` → `Sync Project with Gradle Files`

3. **Connect Device or Start Emulator**
   - **Physical Device**: Enable USB debugging and connect via USB
   - **Emulator**: Create and start an AVD (Android Virtual Device)

4. **Run the App**
   - Click the green "Run" button (or press `Shift+F10`)
   - Select your device
   - Wait for app to install and launch

5. **Configure API URL in App**
   - When the app opens, tap the settings icon (⚙️) in the top-right
   - Paste the Gradio URL from Colab
   - Tap "Save"
   - You should see "API Connected" confirmation

### Part 3: Using the App

1. **Upload Body Photo**
   - Tap "Body Photo" card
   - Choose "Take Photo" or "Choose from Gallery"
   - Select/capture your body photo

2. **Upload Clothing**
   - Tap "Clothing Item" card
   - Choose "Take Photo" or "Choose from Gallery"
   - Select/capture the clothing image

3. **Generate Try-On**
   - Ensure both images are selected
   - Tap "Generate Try-On" button
   - Wait 5-30 seconds for processing
   - View the result below

## Building APK

### Debug APK (for testing)

```bash
./gradlew assembleDebug
```

The APK will be in `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (for distribution)

1. Create a keystore (first time only):
   ```bash
   keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
   ```

2. Add signing config to `app/build.gradle.kts`:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("../my-release-key.jks")
               storePassword = "your-password"
               keyAlias = "my-key-alias"
               keyPassword = "your-password"
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               // ... other settings
           }
       }
   }
   ```

3. Build release APK:
   ```bash
   ./gradlew assembleRelease
   ```

The APK will be in `app/build/outputs/apk/release/app-release.apk`

## Architecture

### Android App (Frontend)

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **State Management**: Kotlin Flows

### AI Backend

- **Platform**: Google Colab (Python)
- **Framework**: Gradio (for API)
- **AI Library**: PyTorch + Diffusers
- **Image Processing**: PIL (Pillow)

### Data Flow

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Android   │─────▶│    Gradio    │─────▶│  AI Model   │
│     App     │ HTTP │   API (Colab)│      │  (Python)   │
└─────────────┘      └──────────────┘      └─────────────┘
      ▲                      │                      │
      │                      │                      │
      └──────────────────────┴──────────────────────┘
              Result Image (JPEG bytes)
```

## Key Components

### TryOnViewModel

Manages app state and business logic:
- Image URIs for body and clothing
- API URL configuration
- Network request handling
- Result state management

### HomeScreen

Main UI with:
- Image upload cards
- Camera/gallery picker
- Generate button
- Result display
- API configuration dialog

### RetrofitClient

HTTP client for API communication:
- Configurable base URL
- Multipart image upload
- Error handling
- Logging for debugging

### Gradio API

Python backend:
- Receives two images via POST
- Processes with AI model
- Returns generated try-on image

## Limitations & Known Issues

### Colab Limitations

- **Session Duration**: ~1-2 hours, then needs restart
- **GPU Time**: Limited free GPU hours per day
- **Public URL**: Changes each session
- **Concurrent Users**: Limited (free tier)

### App Limitations

- **Not Production-Ready**: This is an MVP/demo
- **Simple AI Model**: Uses basic image overlay (replace with real AI)
- **No User Authentication**: No login/accounts
- **No Image History**: Results not saved
- **Network Required**: No offline mode

### Recommended for Production

- Deploy backend to cloud (AWS, GCP, Azure)
- Use production-grade AI model (VITON-HD, etc.)
- Add user authentication (Firebase Auth)
- Implement image storage (Firebase Storage)
- Add caching and optimization
- Improve error handling
- Add analytics

## Upgrading the AI Model

The current implementation uses a simple image overlay. To upgrade to a real AI model:

### Option 1: VITON-HD

```python
# In Colab notebook
!git clone https://github.com/shadow2496/VITON-HD.git
# Follow their setup instructions
```

### Option 2: Stable Diffusion

```python
from diffusers import StableDiffusionInpaintPipeline

pipe = StableDiffusionInpaintPipeline.from_pretrained(
    "runwayml/stable-diffusion-inpainting",
    torch_dtype=torch.float16,
).to("cuda")

# Implement custom try-on logic
```

### Option 3: Commercial APIs

- **Replicate**: https://replicate.com/
- **Hugging Face**: https://huggingface.co/inference-api
- **AWS Sagemaker**: For custom models

## Troubleshooting

### Android App Issues

**Gradle sync fails**
- Check internet connection
- Try `File` → `Invalidate Caches and Restart`
- Update Gradle in `gradle/wrapper/gradle-wrapper.properties`

**App crashes on launch**
- Check Logcat for error messages
- Verify minimum SDK version (24+)
- Ensure all permissions in manifest

**Images not uploading**
- Grant camera/storage permissions
- Check file size (large images may timeout)
- Verify API URL is correct

**Network timeout**
- Increase timeout in `RetrofitClient.kt`
- Check Colab session is still running
- Verify internet connection

### Colab Issues

**GPU not available**
- Check runtime type settings
- Wait if quota exceeded (free tier limit)

**Session disconnected**
- Colab free tier has time limits
- Restart notebook and get new URL

**Out of memory**
- Reduce image sizes
- Use smaller batch size
- Restart runtime

**Module not found**
- Re-run installation cell
- Check for typos in imports

## Performance Tips

### Android

- Compress images before upload (reduce file size)
- Use appropriate image quality settings
- Implement caching for repeated requests
- Add loading indicators for better UX

### Colab

- Use GPU runtime for faster processing
- Preload models to avoid loading on each request
- Implement batch processing if needed
- Consider Colab Pro for longer sessions

## Security Considerations

This is a demo app. For production:

1. **API Security**
   - Add authentication (API keys)
   - Use HTTPS only
   - Implement rate limiting
   - Validate image inputs

2. **Data Privacy**
   - Don't store user images without consent
   - Implement data retention policies
   - Add privacy policy
   - Comply with GDPR/CCPA if applicable

3. **App Security**
   - Obfuscate code (ProGuard)
   - Secure API keys (don't hardcode)
   - Validate all inputs
   - Use certificate pinning for API calls

## Contributing

This is an MVP template. Feel free to:

- Improve the AI model
- Enhance the UI/UX
- Add features (history, sharing, etc.)
- Optimize performance
- Add tests

## License

This project is for educational purposes. Check licenses of:
- Android libraries (Apache 2.0)
- AI models (varies by model)
- Gradio (Apache 2.0)

## Resources

### Android Development
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Docs](https://kotlinlang.org/docs/home.html)
- [Retrofit](https://square.github.io/retrofit/)

### AI/ML
- [Google Colab](https://colab.research.google.com/)
- [Gradio Documentation](https://gradio.app/docs/)
- [Hugging Face](https://huggingface.co/)
- [VITON-HD Paper](https://arxiv.org/abs/2103.16874)

### Tools
- [Android Studio](https://developer.android.com/studio)
- [GitHub](https://github.com/) - for version control

## Support

For issues:
1. Check Troubleshooting section
2. Review Logcat (Android) or Colab output
3. Search Stack Overflow
4. File an issue on GitHub (if applicable)

## Roadmap

Future enhancements:
- [ ] User authentication
- [ ] Image history/favorites
- [ ] Clothing catalog
- [ ] Social sharing
- [ ] Multiple clothing items
- [ ] Body measurements
- [ ] AR preview
- [ ] Recommendation system

---

**Built with ❤️ for learning and experimentation**

Happy coding! 🚀
