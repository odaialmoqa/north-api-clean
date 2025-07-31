#!/bin/bash

echo "🚀 Deploying North Mobile App with Plaid Link Fix"
echo "================================================="

# Check if we're in the right directory
if [ ! -d "mobile-app" ]; then
    echo "❌ Error: mobile-app directory not found. Please run this script from the project root."
    exit 1
fi

# Navigate to mobile app directory
cd mobile-app

echo "📱 Building Android APK..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi

echo "✅ Build successful!"

# Check for connected devices
echo "🔍 Checking for connected Android devices..."
adb devices

# Install the app
echo "📲 Installing app on connected device..."
./gradlew installDebug

if [ $? -ne 0 ]; then
    echo "❌ Installation failed. Please check if a device is connected."
    exit 1
fi

echo "✅ App installed successfully!"
echo ""
echo "🎉 DEPLOYMENT COMPLETE!"
echo "======================"
echo "✅ North Mobile App deployed to Android device"
echo "✅ Plaid Link fix included"
echo "✅ Ready to test bank account connection"
echo ""
echo "📋 NEXT STEPS:"
echo "1. Open the North app on your device"
echo "2. Navigate to the dashboard"
echo "3. Tap 'Connect Bank Account'"
echo "4. Complete the Plaid Link flow"
echo "5. Verify the app shows 'Connected' (not stuck on 'Connecting...')"
echo ""
echo "🐛 DEBUGGING:"
echo "- View logs: adb logcat | grep 'NorthApp'"
echo "- Check Plaid callbacks: Look for PlaidCallbackManager logs"
echo "- Monitor MainActivity: Look for onActivityResult logs"