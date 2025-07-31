const { exec } = require('child_process');

console.log('📱 Mobile App Log Capture Tool');
console.log('=====================================');
console.log('This will capture logs while you test the connection');
console.log('');
console.log('🔧 Instructions:');
console.log('1. Keep this terminal open');
console.log('2. Open your mobile app');
console.log('3. Click "Connect Bank Account"');
console.log('4. Complete the Plaid Link flow');
console.log('5. Watch the logs here for debugging info');
console.log('');
console.log('🚀 Starting log capture...');
console.log('=====================================');

// Clear logs first
exec('adb logcat -c', (error) => {
    if (error) {
        console.log('❌ Failed to clear logs:', error.message);
        return;
    }
    
    console.log('✅ Logs cleared');
    console.log('📱 Now testing the connection...');
    console.log('');
    
    // Start capturing logs
    const logProcess = exec('adb logcat | grep -E "(NorthApp|Plaid|Error|Exception|Processing|Exchanging|Pulling|🔍|✅|❌)"', (error) => {
        if (error) {
            console.log('❌ Log capture failed:', error.message);
        }
    });
    
    logProcess.stdout.on('data', (data) => {
        console.log(data.toString());
    });
    
    logProcess.stderr.on('data', (data) => {
        console.log('Log Error:', data.toString());
    });
    
    // Stop after 2 minutes
    setTimeout(() => {
        console.log('\n⏰ Log capture timeout (2 minutes)');
        logProcess.kill();
        process.exit(0);
    }, 120000);
}); 