# 🚀 START HERE - Complete Setup Guide

**Goal**: Get your Virtual Try-On app running in about 30 minutes!

---

## ⚠️ Prerequisites - Install These First

Since you're on macOS, you need to install Android Studio (it includes everything: Java, Android SDK, emulator, etc.)

### Step 1: Install Android Studio (15 minutes)

1. **Download Android Studio**
   - Go to: https://developer.android.com/studio
   - Click "Download Android Studio"
   - Accept the terms and download

2. **Install Android Studio**
   - Open the downloaded `.dmg` file
   - Drag "Android Studio" to your Applications folder
   - Launch Android Studio from Applications

3. **Complete Setup Wizard**
   - Click "Next" through the welcome screens
   - Choose "Standard" installation
   - Select your preferred theme
   - Wait for SDK components to download (5-10 minutes)
   - Click "Finish" when done

4. **Verify Installation**
   - Android Studio should now be open
   - You should see a "Welcome to Android Studio" screen

---

## 🎯 Quick Start Steps

### Step 2: Set Up AI Backend (5 minutes)

1. **Open Google Colab**
   - Go to: https://colab.research.google.com/
   - Sign in with your Google account

2. **Upload the Notebook**
   - Click "File" → "Upload notebook"
   - Click "Choose File"
   - Navigate to: `/Users/arezoughanekanafi/android apps/TryOn/`
   - Select `VirtualTryOn_Colab.ipynb`
   - Click "Open"

3. **Enable GPU**
   - In Colab, click "Runtime" → "Change runtime type"
   - Under "Hardware accelerator", select "GPU"
   - Click "Save"

4. **Run the Notebook**
   - Click "Runtime" → "Run all"
   - Or click the play button (▶️) on each cell, starting from the top
   - Wait for each cell to finish before the next runs

5. **Get Your API URL**
   - Scroll to the bottom of the notebook
   - Look for output like:
     ```
     Running on public URL: https://xxxxx.gradio.live
     ```
   - **COPY THIS URL** - you'll need it soon!
   - Keep this browser tab open (the URL expires if you close it)

   Example URL: `https://1234abcd5678efgh.gradio.live`

---

### Step 3: Open Project in Android Studio (5 minutes)

1. **Launch Android Studio**
   - Open Android Studio from Applications

2. **Import the Project**
   - On the welcome screen, click "Open"
   - Navigate to: `/Users/arezoughanekanafi/android apps/TryOn`
   - Click "Open"

3. **Trust the Project**
   - If prompted about Gradle sync, click "Trust Project"
   - Android Studio will now sync Gradle dependencies

4. **Wait for Sync to Complete**
   - Look at the bottom of Android Studio for a progress bar
   - This will take 5-10 minutes the first time (downloading dependencies)
   - You'll see "Gradle sync finished" when done

5. **Troubleshooting Sync Issues**
   - If sync fails, click "File" → "Sync Project with Gradle Files"
   - If still failing, click "File" → "Invalidate Caches" → "Invalidate and Restart"

---

### Step 4: Set Up an Android Device (Choose One)

#### Option A: Use a Physical Android Phone (Recommended - Faster)

1. **Enable Developer Options on Your Phone**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - You'll see "You are now a developer!"

2. **Enable USB Debugging**
   - Go to Settings → System → Developer Options
   - Turn on "USB Debugging"

3. **Connect Your Phone**
   - Connect phone to Mac with USB cable
   - On your phone, tap "Allow" when prompted about USB debugging
   - Keep "Always allow from this computer" checked

4. **Verify Connection**
   - In Android Studio, look at the top toolbar
   - You should see your device name in the device dropdown
   - If not visible, click the dropdown and select your device

#### Option B: Use Android Emulator (Slower but works if no phone)

1. **Open Device Manager**
   - In Android Studio, click the phone icon (📱) in the right sidebar
   - Or go to Tools → Device Manager

2. **Create a Virtual Device**
   - Click "Create Device"
   - Select "Pixel 5" (or any phone)
   - Click "Next"

3. **Download System Image**
   - Select "UpsideDownCake" (API 34) or latest
   - Click "Download" next to it
   - Wait for download (takes 5-10 minutes)
   - Click "Finish" when done

4. **Finish Creation**
   - Click "Next"
   - Click "Finish"

5. **Start Emulator**
   - Click the play button (▶️) next to your virtual device
   - Wait for emulator to boot (1-2 minutes)

---

### Step 5: Build and Run the App (2 minutes)

1. **Select Your Device**
   - At the top of Android Studio, click the device dropdown
   - Select your phone or emulator

2. **Click Run**
   - Click the green "Run" button (▶️) at the top
   - Or press `Shift + F10`

3. **Wait for Build**
   - First build takes 2-5 minutes
   - You'll see build progress at the bottom
   - App will automatically install and launch on your device

4. **Success!**
   - You should see "Virtual Try-On" app open on your device
   - If you see the home screen with upload buttons, it worked! 🎉

---

### Step 6: Configure and Test the App (3 minutes)

1. **Open the App**
   - The app should already be open
   - If not, find "Virtual Try-On" in your app drawer and tap it

2. **Configure API URL**
   - Tap the settings icon (⚙️) in the top-right corner
   - A dialog will appear asking for "API URL"
   - Paste the Gradio URL you copied earlier
   - Example: `https://1234abcd5678efgh.gradio.live`
   - Tap "Save"
   - You should see a green "API Connected" message

3. **Test with Images**

   **Upload Body Photo:**
   - Tap the "Body Photo" card
   - Choose "Take Photo" (to use camera) or "Choose from Gallery"
   - If using camera, grant camera permission when prompted
   - Take or select a photo of a person (full body works best)
   - The photo should appear in the card

   **Upload Clothing:**
   - Tap the "Clothing Item" card
   - Choose "Take Photo" or "Choose from Gallery"
   - Take or select a photo of clothing
   - The photo should appear in the card

   **Generate Try-On:**
   - Both images should now be visible
   - The "Generate Try-On" button should be enabled (blue)
   - Tap "Generate Try-On"
   - You'll see a loading spinner
   - Wait 10-30 seconds
   - The result image will appear below!

4. **Success!** 🎉
   - You should see the generated try-on image
   - Try with different photos!

---

## 🎯 Current Status Checklist

- [ ] Android Studio installed
- [ ] Google Colab notebook running
- [ ] Gradio URL copied
- [ ] Project opened in Android Studio
- [ ] Gradle sync completed
- [ ] Device/emulator set up
- [ ] App built and running
- [ ] API URL configured
- [ ] Test images uploaded
- [ ] Try-on generated successfully

---

## 🆘 Common Issues & Solutions

### "Gradle sync failed"
**Solution:**
1. Check your internet connection
2. In Android Studio: File → Invalidate Caches → Invalidate and Restart
3. Try again after restart

### "No devices available"
**Solution:**
- For phone: Make sure USB debugging is enabled and cable is connected
- For emulator: Create and start one (see Step 4, Option B)

### "App won't build"
**Solution:**
1. Make sure Gradle sync finished successfully
2. Click Build → Clean Project
3. Then Build → Rebuild Project
4. Try running again

### "Permission denied for camera/photos"
**Solution:**
- On Android: Settings → Apps → Virtual Try-On → Permissions → Enable Camera and Storage
- Try taking/selecting photos again

### "API connection failed"
**Solution:**
1. Make sure Colab notebook is still running (don't close the tab!)
2. Check the Gradio URL is copied correctly (no extra spaces)
3. Make sure your phone/emulator has internet connection
4. The URL should start with `https://`

### "Processing timeout"
**Solution:**
- Use smaller images (< 5MB)
- Wait longer (can take up to 1 minute)
- Check if Colab session is still active
- Restart Colab if needed

### "Can't find ANDROID_HOME"
**Solution:**
Android Studio handles this automatically. Just:
1. Open Android Studio
2. Go to Tools → SDK Manager
3. Note the "Android SDK Location" path
4. If asked, let Android Studio set it up

---

## 📱 Tips for Best Results

### For Photos:
- Use well-lit photos
- Portrait orientation works best for body photos
- Clear, front-facing clothing images work best
- Avoid very large files (keep under 5MB)

### For the App:
- Keep Colab tab open while using the app
- Colab sessions last ~1 hour, then need restart
- If it stops working, check if Colab session expired
- You can run the notebook again to get a new URL

---

## 🎓 Next Steps (After You Get It Working)

### Improve the AI Model
The current implementation uses a simple overlay. To upgrade:
- See README.md section "Upgrading the AI Model"
- Try VITON-HD or Stable Diffusion models
- Follow instructions in the Colab notebook

### Deploy for Production
- Move from Colab to Cloud Run or AWS
- See BUILD_INSTRUCTIONS.md for release builds
- Add user authentication with Firebase
- Implement image storage

### Customize the App
- Change colors: Edit `app/src/main/java/com/tryon/virtualfit/ui/theme/Color.kt`
- Change text: Edit `app/src/main/res/values/strings.xml`
- Modify UI: Edit `app/src/main/java/com/tryon/virtualfit/ui/screens/HomeScreen.kt`

---

## 📞 Need More Help?

1. **Read the docs:**
   - QUICK_START.md - Alternative quick guide
   - README.md - Complete documentation
   - BUILD_INSTRUCTIONS.md - Detailed build help

2. **Check your environment:**
   ```bash
   ./verify-setup.sh
   ```

3. **Troubleshooting:**
   - See README.md "Troubleshooting" section
   - Check Android Studio's "Build" output for errors
   - Look at Logcat (bottom of Android Studio) for runtime errors

---

## ✅ Success!

If you've completed all steps and can generate a try-on image, congratulations! 🎉

Your Virtual Try-On app is now fully functional!

**What you've accomplished:**
- ✅ Set up complete Android development environment
- ✅ Deployed an AI backend to the cloud (Colab)
- ✅ Built and ran an Android app
- ✅ Integrated frontend and backend
- ✅ Generated your first virtual try-on!

**Now you can:**
- Experiment with different photos
- Share the app with friends (send them the APK)
- Customize the UI and features
- Upgrade to a production AI model
- Learn more about Android development

---

## 🚀 Time Breakdown

| Step | Time |
|------|------|
| Install Android Studio | 15 min |
| Set up Colab backend | 5 min |
| Open project & Gradle sync | 5 min |
| Set up device | 3 min |
| Build and run | 2 min |
| Test the app | 3 min |
| **Total** | **~30 min** |

*Note: First-time setup takes longer due to downloads. Subsequent runs are much faster!*

---

**Ready? Start with Step 1! 👆**

Good luck! 🎉
