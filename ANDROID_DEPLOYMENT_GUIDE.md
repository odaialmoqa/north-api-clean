# North Mobile App - Android Deployment Guide for Pixel 9 Pro

## Prerequisites

### 1. Enable Developer Options on Your Pixel 9 Pro
1. Open **Settings** on your Pixel 9 Pro
2. Scroll down to **About phone**
3. Tap **Build number** 7 times rapidly
4. You'll see "You are now a developer!" message
5. Go back to **Settings** → **System** → **Developer options**
6. Enable **USB debugging**
7. Enable **Install via USB** (if available)

### 2. Install Android SDK Platform Tools (if not already installed)
```bash
# On macOS with Homebrew
brew install android-platform-tools

# Or download from: https://developer.android.com/studio/releases/platform-tools
```

### 3. Connect Your Phone
1. Connect your Pixel 9 Pro to your Mac via USB-C cable
2. On your phone, when prompted, select **File Transfer** mode
3. Allow USB debugging when the dialog appears
4. Check "Always allow from this computer" and tap **OK**

## Deployment Methods

### Method 1: Direct APK Installation (Recommended for Testing)

#### Step 1: Build the Debug APK
```bash
# Navigate to your project directory
cd /path/to/North\ App

# Clean and build debug APK
./gradlew clean
./gradlew composeApp:assembleDebug
```

#### Step 2: Install APK on Your Phone
```bash
# Check if your device is connected
adb devices

# Install the APK
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Or force reinstall if already installed
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### Method 2: Android Studio Installation

#### Step 1: Open Project in Android Studio
1. Open Android Studio
2. Select **Open an existing project**
3. Navigate to your North App directory
4. Select the project root folder

#### Step 2: Configure Device
1. In Android Studio, click the device dropdown (top toolbar)
2. Your Pixel 9 Pro should appear as "Pixel 9 Pro" or similar
3. If not visible, click **Troubleshoot Device Connections**

#### Step 3: Run the App
1. Click the green **Run** button (▶️) or press `Ctrl+R`
2. Select your Pixel 9 Pro from the device list
3. Android Studio will build and install the app automatically

### Method 3: Gradle Direct Run (Fastest for Development)

```bash
# Run directly on connected device
./gradlew composeApp:installDebug

# Or run and launch the app
./gradlew composeApp:installDebug && adb shell am start -n com.north.mobile/.MainActivity
```

## Troubleshooting Common Issues

### Issue 1: Device Not Recognized
```bash
# Check if device is connected
adb devices

# If no devices shown:
# 1. Try different USB cable
# 2. Enable "File Transfer" mode on phone
# 3. Revoke USB debugging authorizations in Developer Options, then reconnect
# 4. Restart adb server
adb kill-server
adb start-server
```

### Issue 2: Installation Failed
```bash
# If installation fails, try:
adb uninstall com.north.mobile
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Or check for specific error
adb install -r -d composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### Issue 3: App Crashes on Launch
```bash
# View crash logs
adb logcat | grep -i "north\|crash\|error"

# Or filter for your app specifically
adb logcat | grep com.north.mobile
```

### Issue 4: Gradle Build Fails
```bash
# Clean and rebuild
./gradlew clean
./gradlew composeApp:assembleDebug --stacktrace

# If still fails, check Java version
java -version
# Should be Java 17 or higher
```

## Testing Features on Your Pixel 9 Pro

### 1. Core App Features to Test
- [ ] **App Launch**: Verify app starts without crashes
- [ ] **Onboarding Flow**: Complete the welcome and setup process
- [ ] **Biometric Authentication**: Test fingerprint/face unlock
- [ ] **UI Responsiveness**: Navigate through all screens
- [ ] **Animations**: Check gamification celebrations work smoothly
- [ ] **Notifications**: Test push notification display
- [ ] **Offline Mode**: Test app behavior without internet

### 2. Pixel 9 Pro Specific Features
- [ ] **High Refresh Rate**: Verify smooth 120Hz animations
- [ ] **Edge-to-Edge Display**: Check UI adapts to screen edges
- [ ] **Dynamic Color**: Test Material You theming
- [ ] **Adaptive Brightness**: Test in different lighting conditions
- [ ] **Gesture Navigation**: Verify app works with gesture nav
- [ ] **Split Screen**: Test app in multi-window mode

### 3. Performance Testing
```bash
# Monitor app performance while testing
adb shell top | grep com.north.mobile

# Check memory usage
adb shell dumpsys meminfo com.north.mobile

# Monitor battery usage
adb shell dumpsys batterystats | grep com.north.mobile
```

## Development Workflow

### 1. Quick Development Cycle
```bash
# Make code changes, then:
./gradlew composeApp:installDebug

# App will automatically update on your phone
```

### 2. Debug with Logs
```bash
# View real-time logs while testing
adb logcat -s "NorthApp"

# Or view all logs with timestamp
adb logcat -v time
```

### 3. Take Screenshots for Documentation
```bash
# Capture screenshot
adb exec-out screencap -p > screenshot.png

# Or record screen video
adb shell screenrecord /sdcard/demo.mp4
# Stop recording with Ctrl+C, then pull the file:
adb pull /sdcard/demo.mp4
```

## Advanced Testing

### 1. Test Different Network Conditions
```bash
# Simulate slow network
adb shell settings put global airplane_mode_on 1
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true

# Restore network
adb shell settings put global airplane_mode_on 0
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false
```

### 2. Test App Permissions
- Test location permissions (if used)
- Test notification permissions
- Test biometric permissions
- Test camera permissions (if used)

### 3. Test App States
- Test app in background/foreground transitions
- Test app after phone restart
- Test app with low memory conditions
- Test app with different system languages

## Release Build (For Final Testing)

### Build Release APK
```bash
# Build release APK (unsigned for testing)
./gradlew composeApp:assembleRelease

# Install release build
adb install composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk
```

### Performance Comparison
```bash
# Compare debug vs release performance
# Debug build:
./gradlew composeApp:installDebug
# Test performance, then:

# Release build:
./gradlew composeApp:installRelease
# Test performance again and compare
```

## Useful ADB Commands for Testing

```bash
# Launch app
adb shell am start -n com.north.mobile/.MainActivity

# Force stop app
adb shell am force-stop com.north.mobile

# Clear app data
adb shell pm clear com.north.mobile

# Uninstall app
adb uninstall com.north.mobile

# Check app info
adb shell dumpsys package com.north.mobile

# Monitor app in real-time
adb shell top | grep com.north.mobile
```

## Quick Start Commands

Here's the fastest way to get the app running on your Pixel 9 Pro:

```bash
# 1. Connect your phone and enable USB debugging
# 2. Run these commands:

cd "/path/to/North App"
./gradlew clean
./gradlew composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk

# The app should now be installed and ready to test!
```

## Expected Results

After successful installation, you should see:
- ✅ "North" app icon in your app drawer
- ✅ App launches without crashes
- ✅ Smooth animations and transitions
- ✅ Responsive UI that adapts to Pixel 9 Pro's display
- ✅ All features working as designed

## Next Steps After Testing

1. **Report Issues**: Note any bugs or performance issues
2. **Test User Flows**: Go through complete onboarding and key features
3. **Performance Monitoring**: Check battery usage and memory consumption
4. **UI/UX Feedback**: Evaluate user experience on the device
5. **Feature Validation**: Ensure all implemented features work correctly

---

**Ready to deploy!** Follow the Quick Start Commands above to get the North Mobile App running on your Pixel 9 Pro. 📱✨