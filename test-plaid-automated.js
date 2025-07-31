const { exec } = require('child_process');
const fs = require('fs');

class PlaidAutomatedTester {
    constructor() {
        this.testCount = 0;
        this.maxTests = 10;
    }

    async runTest() {
        this.testCount++;
        console.log(`\n🧪 TEST #${this.testCount}: Automated Plaid Link Test`);
        
        try {
            // 1. Clear logs
            await this.clearLogs();
            
            // 2. Trigger button press via ADB
            await this.triggerButtonPress();
            
            // 3. Wait and capture logs
            await this.captureLogs();
            
            // 4. Analyze results
            await this.analyzeResults();
            
        } catch (error) {
            console.error(`❌ Test #${this.testCount} failed:`, error.message);
        }
    }

    async clearLogs() {
        console.log('📝 Clearing logs...');
        return new Promise((resolve) => {
            exec('adb logcat -c', (error) => {
                if (error) console.log('⚠️  Could not clear logs:', error.message);
                resolve();
            });
        });
    }

    async triggerButtonPress() {
        console.log('🔘 Triggering button press...');
        return new Promise((resolve) => {
            // Use ADB to tap the button coordinates (approximate)
            // We'll need to find the actual coordinates
            exec('adb shell input tap 480 1200', (error) => {
                if (error) {
                    console.log('⚠️  Could not trigger button press:', error.message);
                } else {
                    console.log('✅ Button press triggered');
                }
                resolve();
            });
        });
    }

    async captureLogs() {
        console.log('📊 Capturing logs for 10 seconds...');
        return new Promise((resolve) => {
            const timeout = setTimeout(() => {
                process.kill(logProcess.pid);
                resolve();
            }, 10000);

            const logProcess = exec('adb logcat | grep -E "(NorthApp|🔧|🔘|🔄|🚀|✅|❌|Plaid|Link|DEBUG|AndroidRuntime|FATAL|ERROR|System.out)"', (error, stdout, stderr) => {
                clearTimeout(timeout);
                this.logs = stdout;
                resolve();
            });
        });
    }

    async analyzeResults() {
        console.log('🔍 Analyzing results...');
        
        if (!this.logs) {
            console.log('❌ No logs captured');
            return;
        }

        const logLines = this.logs.split('\n').filter(line => line.trim());
        
        // Check for key indicators
        const hasButtonClick = logLines.some(line => line.includes('🔘 Dashboard Plaid button clicked!'));
        const hasPlaidLaunch = logLines.some(line => line.includes('🚀 Launching Plaid Link'));
        const hasPlaidUI = logLines.some(line => line.includes('Plaid') || line.includes('Link'));
        const hasError = logLines.some(line => line.includes('❌') || line.includes('ERROR') || line.includes('FATAL'));
        
        console.log(`📊 Results:
  - Button clicked: ${hasButtonClick ? '✅' : '❌'}
  - Plaid launched: ${hasPlaidLaunch ? '✅' : '❌'}
  - Plaid UI detected: ${hasPlaidUI ? '✅' : '❌'}
  - Errors: ${hasError ? '❌' : '✅'}`);

        if (hasButtonClick && hasPlaidLaunch && !hasError) {
            console.log('🎉 SUCCESS: Plaid Link appears to be working!');
            return true;
        } else {
            console.log('❌ FAILURE: Plaid Link not working properly');
            return false;
        }
    }

    async runContinuousTests() {
        console.log('🚀 Starting automated Plaid testing...');
        
        for (let i = 0; i < this.maxTests; i++) {
            const success = await this.runTest();
            if (success) {
                console.log('🎉 Plaid Link is working! Stopping tests.');
                break;
            }
            
            if (i < this.maxTests - 1) {
                console.log('⏳ Waiting 5 seconds before next test...');
                await new Promise(resolve => setTimeout(resolve, 5000));
            }
        }
    }
}

// Run the automated tester
const tester = new PlaidAutomatedTester();
tester.runContinuousTests().catch(console.error); 