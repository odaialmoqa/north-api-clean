#!/bin/bash

# North App Deployment Script
# This script builds the Android APK and prepares it for installation

echo "🚀 Starting North App Deployment Process..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    print_error "Please run this script from the root of your North app project"
    exit 1
fi

print_status "Cleaning previous builds..."
./gradlew clean

print_status "Building Android APK..."
./gradlew :composeApp:assembleDebug

# Check if build was successful
if [ $? -eq 0 ]; then
    print_success "Android APK built successfully!"
    
    # Find the APK file
    APK_PATH=$(find . -name "*.apk" -path "*/composeApp/build/outputs/apk/debug/*" | head -1)
    
    if [ -n "$APK_PATH" ]; then
        print_success "APK located at: $APK_PATH"
        
        # Copy APK to a convenient location
        cp "$APK_PATH" "./north-app-debug.apk"
        print_success "APK copied to: ./north-app-debug.apk"
        
        # Get APK info
        APK_SIZE=$(du -h "./north-app-debug.apk" | cut -f1)
        print_status "APK size: $APK_SIZE"
        
        echo ""
        print_success "🎉 Deployment ready!"
        echo ""
        echo "📱 To install on your phone:"
        echo "   1. Enable 'Developer Options' and 'USB Debugging' on your Android device"
        echo "   2. Connect your phone via USB"
        echo "   3. Run: adb install -r ./north-app-debug.apk"
        echo "   OR"
        echo "   4. Transfer ./north-app-debug.apk to your phone and install manually"
        echo ""
        echo "🌐 Server deployment:"
        echo "   Your server is ready to deploy with the enhanced AI CFO endpoints"
        echo "   Make sure to set these environment variables:"
        echo "   - DATABASE_URL (your PostgreSQL connection string)"
        echo "   - JWT_SECRET (a secure random string)"
        echo ""
        
    else
        print_error "Could not find the built APK file"
        exit 1
    fi
else
    print_error "Build failed! Check the error messages above."
    exit 1
fi