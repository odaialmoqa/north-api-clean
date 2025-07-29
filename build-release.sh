#!/bin/bash

echo "Building North Financial App for Google Play..."

# Navigate to mobile app directory
cd mobile-app

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Build release AAB (Android App Bundle) - preferred for Play Store
echo "Building release AAB..."
./gradlew bundleRelease

# Build release APK (for testing)
echo "Building release APK..."
./gradlew assembleRelease

echo "Build complete!"
echo "AAB location: mobile-app/composeApp/build/outputs/bundle/release/composeApp-release.aab"
echo "APK location: mobile-app/composeApp/build/outputs/apk/release/composeApp-release.apk"

# Check if files exist
if [ -f "composeApp/build/outputs/bundle/release/composeApp-release.aab" ]; then
    echo "✅ AAB build successful"
else
    echo "❌ AAB build failed"
fi

if [ -f "composeApp/build/outputs/apk/release/composeApp-release.apk" ]; then
    echo "✅ APK build successful"
else
    echo "❌ APK build failed"
fi