const { exec } = require('child_process');

console.log('🧪 Simple Plaid Link Test');
console.log('========================');

async function testPlaidLink() {
    try {
        // 1. Check if device is connected
        console.log('📱 Checking device connection...');
        const devices = await execCommand('adb devices');
        if (!devices.includes('device')) {
            console.log('❌ No device connected. Please connect your device and try again.');
            return;
        }
        console.log('✅ Device connected');

        // 2. Install the app
        console.log('📦 Installing app...');
        await execCommand('./gradlew assembleDebug && adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk');
        console.log('✅ App installed');

        // 3. Clear logs
        console.log('📝 Clearing logs...');
        await execCommand('adb logcat -c');
        console.log('✅ Logs cleared');

        // 4. Launch the app
        console.log('🚀 Launching app...');
        await execCommand('adb shell am start -n com.north.mobile.debug/com.north.mobile.MainActivity');
        console.log('✅ App launched');

        // 5. Wait a moment for app to load
        console.log('⏳ Waiting for app to load...');
        await sleep(3000);

        // 6. Tap the "Test Plaid Link" button
        console.log('🔘 Tapping "Test Plaid Link" button...');
        await execCommand('adb shell input tap 480 800');
        console.log('✅ Button tapped');

        // 7. Capture logs for 10 seconds
        console.log('📊 Capturing logs for 10 seconds...');
        const logs = await captureLogs(10000);
        
        // 8. Analyze results
        console.log('🔍 Analyzing results...');
        analyzeLogs(logs);

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

function execCommand(command) {
    return new Promise((resolve, reject) => {
        exec(command, (error, stdout, stderr) => {
            if (error) {
                reject(error);
            } else {
                resolve(stdout);
            }
        });
    });
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function captureLogs(duration) {
    return new Promise((resolve) => {
        let logProcess;
        const timeout = setTimeout(() => {
            if (logProcess) {
                logProcess.kill();
            }
            resolve('');
        }, duration);

        logProcess = exec('adb logcat | grep -E "(NorthApp|🔧|🔘|🔄|🚀|✅|❌|Plaid|Link|DEBUG|AndroidRuntime|FATAL|ERROR|System.out)"', (error, stdout, stderr) => {
            clearTimeout(timeout);
            resolve(stdout);
        });
    });
}

function analyzeLogs(logs) {
    if (!logs) {
        console.log('❌ No logs captured');
        return;
    }

    const logLines = logs.split('\n').filter(line => line.trim());
    
    const hasPlaidLaunch = logLines.some(line => line.includes('🔧 Creating Plaid configuration'));
    const hasPlaidOpen = logLines.some(line => line.includes('Plaid.open() returned: true'));
    const hasPlaidUI = logLines.some(line => line.includes('Plaid') || line.includes('Link'));
    const hasError = logLines.some(line => line.includes('❌') || line.includes('ERROR') || line.includes('FATAL'));
    
    console.log(`📊 Results:
  - Plaid launched: ${hasPlaidLaunch ? '✅' : '❌'}
  - Plaid opened successfully: ${hasPlaidOpen ? '✅' : '❌'}
  - Plaid UI detected: ${hasPlaidUI ? '✅' : '❌'}
  - Errors: ${hasError ? '❌' : '✅'}`);

    if (hasPlaidLaunch && hasPlaidOpen && !hasError) {
        console.log('🎉 SUCCESS: Plaid Link appears to be working!');
    } else {
        console.log('❌ FAILURE: Plaid Link not working properly');
        console.log('\n📋 Recent logs:');
        logLines.slice(-10).forEach(line => console.log(`  ${line}`));
    }
}

// Run the test
testPlaidLink().catch(console.error); 