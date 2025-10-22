#!/bin/bash

# Virtual Try-On App - Setup Verification Script
# This script checks if your development environment is ready

echo "🔍 Virtual Try-On App - Setup Verification"
echo "==========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check functions
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $2 found"
        return 0
    else
        echo -e "${RED}✗${NC} $2 not found"
        return 1
    fi
}

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $2 exists"
        return 0
    else
        echo -e "${RED}✗${NC} $2 missing"
        return 1
    fi
}

check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $2 exists"
        return 0
    else
        echo -e "${RED}✗${NC} $2 missing"
        return 1
    fi
}

# Track overall status
all_good=true

# Check 1: Java/JDK
echo "1. Checking Java Development Kit (JDK)..."
if check_command java "Java"; then
    java_version=$(java -version 2>&1 | head -n 1)
    echo "   Version: $java_version"
else
    echo -e "   ${YELLOW}⚠${NC}  Install JDK 8 or higher from: https://www.oracle.com/java/technologies/downloads/"
    all_good=false
fi
echo ""

# Check 2: Android SDK
echo "2. Checking Android SDK..."
if [ ! -z "$ANDROID_HOME" ]; then
    echo -e "${GREEN}✓${NC} ANDROID_HOME set to: $ANDROID_HOME"
    if [ -d "$ANDROID_HOME" ]; then
        echo -e "${GREEN}✓${NC} Android SDK directory exists"
    else
        echo -e "${RED}✗${NC} ANDROID_HOME path does not exist"
        all_good=false
    fi
else
    echo -e "${YELLOW}⚠${NC}  ANDROID_HOME not set"
    echo "   Set it in your ~/.bash_profile or ~/.zshrc:"
    echo "   export ANDROID_HOME=\$HOME/Library/Android/sdk"
    echo "   export PATH=\$PATH:\$ANDROID_HOME/tools:\$ANDROID_HOME/platform-tools"
    all_good=false
fi
echo ""

# Check 3: ADB (Android Debug Bridge)
echo "3. Checking Android Debug Bridge (ADB)..."
if check_command adb "ADB"; then
    adb_version=$(adb --version | head -n 1)
    echo "   Version: $adb_version"

    # Check for connected devices
    connected=$(adb devices | grep -v "List" | grep "device" | wc -l)
    if [ $connected -gt 0 ]; then
        echo -e "${GREEN}✓${NC} $connected Android device(s) connected"
        adb devices | grep "device" | grep -v "List"
    else
        echo -e "${YELLOW}⚠${NC}  No Android devices connected"
        echo "   Connect a device or start an emulator to test the app"
    fi
else
    echo -e "   ${YELLOW}⚠${NC}  Install Android Studio which includes ADB"
    all_good=false
fi
echo ""

# Check 4: Gradle
echo "4. Checking Gradle..."
if check_file "./gradlew" "Gradle wrapper"; then
    if [ -x "./gradlew" ]; then
        echo -e "${GREEN}✓${NC} Gradle wrapper is executable"
    else
        echo -e "${YELLOW}⚠${NC}  Making gradlew executable..."
        chmod +x ./gradlew
        echo -e "${GREEN}✓${NC} Fixed"
    fi
else
    echo -e "${RED}✗${NC} Gradle wrapper not found"
    all_good=false
fi
echo ""

# Check 5: Project Structure
echo "5. Checking project structure..."
check_dir "app/src/main/java/com/tryon/virtualfit" "Source directory"
check_file "app/src/main/AndroidManifest.xml" "AndroidManifest.xml"
check_file "app/build.gradle.kts" "App build file"
check_file "build.gradle.kts" "Root build file"
check_file "settings.gradle.kts" "Settings file"
echo ""

# Check 6: Essential Kotlin files
echo "6. Checking essential Kotlin files..."
check_file "app/src/main/java/com/tryon/virtualfit/MainActivity.kt" "MainActivity"
check_file "app/src/main/java/com/tryon/virtualfit/viewmodel/TryOnViewModel.kt" "TryOnViewModel"
check_file "app/src/main/java/com/tryon/virtualfit/ui/screens/HomeScreen.kt" "HomeScreen"
echo ""

# Check 7: Resources
echo "7. Checking resource files..."
check_file "app/src/main/res/values/strings.xml" "strings.xml"
check_file "app/src/main/res/values/themes.xml" "themes.xml"
check_file "app/src/main/res/xml/file_paths.xml" "file_paths.xml"
echo ""

# Check 8: Backend notebook
echo "8. Checking AI backend..."
check_file "VirtualTryOn_Colab.ipynb" "Colab notebook"
echo ""

# Check 9: Documentation
echo "9. Checking documentation..."
check_file "README.md" "README"
check_file "QUICK_START.md" "Quick Start Guide"
check_file "BUILD_INSTRUCTIONS.md" "Build Instructions"
echo ""

# Check 10: Internet connectivity
echo "10. Checking internet connection..."
if ping -c 1 google.com &> /dev/null; then
    echo -e "${GREEN}✓${NC} Internet connection available"
else
    echo -e "${YELLOW}⚠${NC}  No internet connection detected"
    echo "   Internet is required to download Gradle dependencies"
fi
echo ""

# Summary
echo "==========================================="
if [ "$all_good" = true ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    echo "🚀 You're ready to build the app!"
    echo ""
    echo "Next steps:"
    echo "1. Open Android Studio and import this project"
    echo "2. Wait for Gradle sync to complete"
    echo "3. Click the Run button (▶️)"
    echo ""
    echo "Or build from command line:"
    echo "  ./gradlew assembleDebug"
    echo ""
    echo "For detailed instructions, see:"
    echo "  - QUICK_START.md (for a 15-minute setup)"
    echo "  - BUILD_INSTRUCTIONS.md (for detailed build info)"
else
    echo -e "${YELLOW}⚠ Some checks failed${NC}"
    echo ""
    echo "Please fix the issues marked with ✗ or ⚠ above"
    echo "Then run this script again to verify"
    echo ""
    echo "For help, see:"
    echo "  - BUILD_INSTRUCTIONS.md"
    echo "  - README.md (Troubleshooting section)"
fi
echo "==========================================="
