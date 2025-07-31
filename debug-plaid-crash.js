// Debug script for Plaid crash investigation
const { exec } = require('child_process');
const util = require('util');
const execAsync = util.promisify(exec);

console.log('🔍 Plaid Crash Debug Tool');
console.log('=' .repeat(50));

async function monitorLogs() {
    console.log('\n📱 Monitoring Android logs for Plaid-related activity...');
    console.log('Press Ctrl+C to stop monitoring');
    console.log('');
    
    try {
        // Start monitoring logs
        const logProcess = exec('adb logcat | grep -E "(Plaid|North|AndroidRuntime|FATAL|ERROR)"', {
            stdio: 'inherit'
        });
        
        if (logProcess.stdout) {
            logProcess.stdout.on('data', (data) => {
                console.log(data);
            });
        }
        
        if (logProcess.stderr) {
            logProcess.stderr.on('data', (data) => {
                console.log(data);
            });
        }
        
    } catch (error) {
        console.log('❌ Error monitoring logs:', error.message);
    }
}

async function clearLogs() {
    try {
        await execAsync('adb logcat -c');
        console.log('✅ Logs cleared');
    } catch (error) {
        console.log('❌ Failed to clear logs:', error.message);
    }
}

async function testBackend() {
    console.log('\n🧪 Testing backend connectivity...');
    
    try {
        const axios = require('axios');
        const response = await axios.post('https://north-api-clean-production.up.railway.app/api/plaid/create-link-token', {
            user_id: 'test-user-' + Date.now()
        });
        
        if (response.data.link_token) {
            console.log('✅ Backend is working');
            console.log(`🔑 Token: ${response.data.link_token.substring(0, 30)}...`);
        } else {
            console.log('❌ Backend returned invalid response');
        }
    } catch (error) {
        console.log('❌ Backend test failed:', error.response && error.response.data || error.message);
    }
}

async function checkDevice() {
    try {
        const { stdout } = await execAsync('adb devices');
        console.log('📱 Connected devices:');
        console.log(stdout);
    } catch (error) {
        console.log('❌ Failed to check devices:', error.message);
    }
}

async function launchApp() {
    try {
        await execAsync('adb shell am start -n com.north.mobile.debug/.MainActivity');
        console.log('🚀 App launched');
    } catch (error) {
        console.log('❌ Failed to launch app:', error.message);
    }
}

async function runDebugSequence() {
    console.log('\n🔧 Running debug sequence...');
    
    await checkDevice();
    await clearLogs();
    await testBackend();
    await launchApp();
    
    console.log('\n📋 Debug Instructions:');
    console.log('1. The app should now be open on your device');
    console.log('2. Navigate to the Accounts section');
    console.log('3. Tap "Connect Bank Account"');
    console.log('4. Watch the logs below for any crash information');
    console.log('5. If it crashes, the logs will show the exact error');
    console.log('');
    
    console.log('🔍 Common crash causes to look for:');
    console.log('- NullPointerException in PlaidLinkLauncher');
    console.log('- ClassNotFoundException for Plaid classes');
    console.log('- Network connectivity issues');
    console.log('- Context/Activity not found');
    console.log('- Permission denied errors');
    console.log('');
    
    console.log('📱 Starting log monitoring...');
    await monitorLogs();
}

// Handle command line arguments
const args = process.argv.slice(2);

if (args.includes('--help') || args.includes('-h')) {
    console.log('Usage: node debug-plaid-crash.js [options]');
    console.log('');
    console.log('Options:');
    console.log('  --clear-logs    Clear Android logs');
    console.log('  --test-backend  Test backend connectivity');
    console.log('  --check-device  Check connected devices');
    console.log('  --launch-app    Launch the app');
    console.log('  --monitor       Monitor logs only');
    console.log('  --full          Run full debug sequence');
    console.log('');
    process.exit(0);
}

if (args.includes('--clear-logs')) {
    clearLogs();
} else if (args.includes('--test-backend')) {
    testBackend();
} else if (args.includes('--check-device')) {
    checkDevice();
} else if (args.includes('--launch-app')) {
    launchApp();
} else if (args.includes('--monitor')) {
    monitorLogs();
} else {
    // Run full debug sequence by default
    runDebugSequence();
} 