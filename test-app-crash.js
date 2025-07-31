// Test script to help debug Plaid app crash
const axios = require('axios');

console.log('🔍 Plaid App Crash Debug Guide');
console.log('=' .repeat(50));

console.log('\n📱 To test the mobile app Plaid integration:');
console.log('1. Build and install the app:');
console.log('   cd mobile-app && ./gradlew installDebug');
console.log('');
console.log('2. Open the app and navigate to the Accounts section');
console.log('3. Tap "Connect Bank Account"');
console.log('4. Check the Android logs for any crash details:');
console.log('   adb logcat | grep -E "(Plaid|North|AndroidRuntime)"');
console.log('');

console.log('🔧 Common crash causes and fixes:');
console.log('');
console.log('1. Plaid SDK not properly initialized:');
console.log('   - Check that Plaid SDK is included in build.gradle.kts');
console.log('   - Verify the SDK version is compatible (4.1.0)');
console.log('');
console.log('2. Missing permissions:');
console.log('   - Ensure INTERNET permission is in AndroidManifest.xml');
console.log('   - Check for any additional Plaid-specific permissions');
console.log('');
console.log('3. Context/Activity issues:');
console.log('   - The app might be crashing when trying to find the Activity context');
console.log('   - Check if the context is properly passed to PlaidLinkLauncher');
console.log('');
console.log('4. Network connectivity:');
console.log('   - Ensure the device has internet access');
console.log('   - Check if the backend API is accessible');
console.log('');

console.log('🧪 Quick backend test:');
console.log('Running backend connectivity test...');

async function testBackend() {
    try {
        const response = await axios.post('https://north-api-clean-production.up.railway.app/api/plaid/create-link-token', {
            user_id: 'test-user-' + Date.now()
        });
        
        if (response.data.link_token) {
            console.log('✅ Backend is working - link token created successfully');
            console.log(`🔑 Token: ${response.data.link_token.substring(0, 30)}...`);
        } else {
            console.log('❌ Backend returned invalid response');
        }
    } catch (error) {
        console.log('❌ Backend test failed:', error.response && error.response.data || error.message);
    }
}

testBackend();

console.log('\n📋 Debug checklist:');
console.log('□ App builds successfully (✅ confirmed)');
console.log('□ Backend API is accessible');
console.log('□ Plaid SDK is properly included');
console.log('□ Android permissions are set');
console.log('□ Context/Activity handling is correct');
console.log('□ Network connectivity is available');
console.log('□ No null pointer exceptions in PlaidLinkLauncher');
console.log('');

console.log('🚀 Next steps:');
console.log('1. Install the app on a device/emulator');
console.log('2. Test the "Connect Bank Account" button');
console.log('3. Check Android logs for crash details');
console.log('4. If it still crashes, share the crash log'); 