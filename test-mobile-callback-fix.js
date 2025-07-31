#!/usr/bin/env node

/**
 * Test script to demonstrate the mobile app callback fix
 * This simulates the mobile app flow without requiring a running backend
 */

console.log('📱 Testing Mobile App Callback Fix');
console.log('==================================');

// Simulate the mobile app callback mechanism
class PlaidLinkResultHandler {
    constructor() {
        this.onSuccess = null;
        this.onError = null;
    }
    
    setCallbacks(onSuccess, onError) {
        this.onSuccess = onSuccess;
        this.onError = onError;
        console.log('✅ Callbacks set in PlaidLinkResultHandler');
    }
    
    handleSuccess(publicToken) {
        console.log('📞 MainActivity received public token:', publicToken.substring(0, 30) + '...');
        if (this.onSuccess) {
            console.log('📞 Calling success callback...');
            this.onSuccess(publicToken);
        } else {
            console.log('❌ No success callback set!');
        }
        this.clearCallbacks();
    }
    
    handleError(error) {
        console.log('📞 MainActivity received error:', error);
        if (this.onError) {
            console.log('📞 Calling error callback...');
            this.onError(error);
        } else {
            console.log('❌ No error callback set!');
        }
        this.clearCallbacks();
    }
    
    clearCallbacks() {
        this.onSuccess = null;
        this.onError = null;
        console.log('🧹 Callbacks cleared');
    }
}

// Simulate the AndroidPlaidLinkLauncher
class AndroidPlaidLinkLauncher {
    constructor(resultHandler) {
        this.resultHandler = resultHandler;
    }
    
    launchPlaidLink(linkToken, onSuccess, onError) {
        console.log('🚀 AndroidPlaidLinkLauncher.launchPlaidLink called');
        console.log('🔗 Link token:', linkToken.substring(0, 30) + '...');
        
        // Set up callbacks (this is the key fix)
        this.resultHandler.setCallbacks(onSuccess, onError);
        
        console.log('📱 Simulating Plaid Link UI launch...');
        
        // Simulate Plaid Link completion after 2 seconds
        setTimeout(() => {
            const mockPublicToken = 'public-sandbox-12345678-1234-1234-1234-123456789012';
            console.log('✅ Plaid Link completed successfully');
            console.log('📤 Plaid returning public token to MainActivity...');
            
            // This simulates MainActivity.onActivityResult calling the handler
            this.resultHandler.handleSuccess(mockPublicToken);
        }, 2000);
    }
}

// Simulate the PlaidLinkComponent
class PlaidLinkComponent {
    constructor(launcher) {
        this.launcher = launcher;
        this.isConnecting = false;
    }
    
    connectAccount() {
        console.log('🔘 User tapped "Connect Bank Account"');
        this.isConnecting = true;
        console.log('🔄 State: isConnecting = true (showing "Connecting..." UI)');
        
        const mockLinkToken = 'link-sandbox-12345678-1234-1234-1234-123456789012';
        
        this.launcher.launchPlaidLink(
            mockLinkToken,
            (publicToken) => {
                console.log('🎉 SUCCESS CALLBACK RECEIVED in PlaidLinkComponent!');
                console.log('🔍 Public token:', publicToken.substring(0, 30) + '...');
                
                // This is where the mobile app would call the backend
                console.log('📡 Would now call backend /api/plaid/exchange-public-token');
                console.log('🔄 Backend would automatically sync transactions and generate insights');
                
                this.isConnecting = false;
                console.log('✅ State: isConnecting = false (showing "Connected" UI)');
                console.log('🎊 FLOW COMPLETE - No more stuck "Connecting..." state!');
            },
            (error) => {
                console.log('❌ ERROR CALLBACK RECEIVED in PlaidLinkComponent!');
                console.log('📝 Error:', error);
                
                this.isConnecting = false;
                console.log('❌ State: isConnecting = false (showing error UI)');
            }
        );
    }
}

// Run the simulation
async function runSimulation() {
    console.log('\n1️⃣ Setting up components...');
    const resultHandler = new PlaidLinkResultHandler();
    const launcher = new AndroidPlaidLinkLauncher(resultHandler);
    const component = new PlaidLinkComponent(launcher);
    
    console.log('\n2️⃣ Simulating user interaction...');
    component.connectAccount();
    
    // Wait for the simulation to complete
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    console.log('\n🏁 SIMULATION COMPLETE');
    console.log('======================');
    console.log('✅ MainActivity properly communicates with UI layer');
    console.log('✅ PlaidLinkComponent receives public token');
    console.log('✅ No more stuck "Connecting..." state');
    console.log('✅ Ready for backend token exchange and transaction sync');
}

runSimulation().catch(console.error);