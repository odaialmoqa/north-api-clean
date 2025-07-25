#!/bin/bash

# North Mobile App - Pixel 9 Pro Deployment Script

echo "🚀 Building and deploying North Mobile App to Pixel 9 Pro..."

# Check if ADB is installed
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Installing Android platform tools..."
    brew install android-platform-tools
fi

# Check for connected devices
echo "📱 Checking for connected devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device")

if [ -z "$DEVICES" ]; then
    echo "❌ No devices found. Please connect your Pixel 9 Pro and enable USB debugging."
    exit 1
fi

echo "✅ Device found: $DEVICES"

# Build the app
echo "🔨 Building app..."
cd mobile-app
./gradlew composeApp:assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build failed. See errors above."
    exit 1
fi

# Install the app
echo "📲 Installing app on Pixel 9 Pro..."
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk

if [ $? -ne 0 ]; then
    echo "❌ Installation failed. See errors above."
    exit 1
fi

# Launch the app
echo "🚀 Launching app..."
adb shell am start -n com.north.mobile/.MainActivity

echo "✅ North Mobile App successfully deployed to your Pixel 9 Pro!"
echo "📱 Check your device to see the app running."