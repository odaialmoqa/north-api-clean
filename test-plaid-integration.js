// Test Plaid backend integration
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testPlaidIntegration() {
    console.log('🏦 Testing Plaid Integration...');
    console.log('=' .repeat(50));
    
    try {
        // Step 1: Test backend link token creation
        console.log('\n🔗 Step 1: Testing Link Token Creation...');
        
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            user_id: `test-user-${Date.now()}`
        });
        
        if (linkTokenResponse.data.link_token) {
            console.log('✅ Link token created successfully');
            console.log(`🔑 Token: ${linkTokenResponse.data.link_token.substring(0, 30)}...`);
            
            // Step 2: Test public token exchange (simulate)
            console.log('\n🔄 Step 2: Testing Public Token Exchange...');
            
            const testPublicToken = 'public-sandbox-test-token-' + Date.now();
            
            try {
                const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                    public_token: testPublicToken
                });
                
                console.log('✅ Public token exchange endpoint is available');
                console.log('Response:', exchangeResponse.data);
                
            } catch (exchangeError) {
                console.log('⚠️ Public token exchange failed (expected for test token):', 
                    exchangeError.response && exchangeError.response.data);
            }
            
            // Step 3: Test account retrieval
            console.log('\n📊 Step 3: Testing Account Retrieval...');
            
            try {
                const accountsResponse = await axios.get(`${BASE_URL}/api/plaid/accounts`);
                console.log('✅ Account retrieval endpoint is available');
                console.log('Accounts:', accountsResponse.data);
                
            } catch (accountsError) {
                console.log('⚠️ Account retrieval failed (expected if no accounts):', 
                    accountsError.response && accountsError.response.data);
            }
            
        } else {
            console.log('❌ Link token creation failed');
            console.log('Response:', linkTokenResponse.data);
        }
        
    } catch (error) {
        console.log('❌ Plaid integration test failed:');
        console.log('Error:', error.response && error.response.data || error.message);
        
        if (error.response && error.response.status === 401) {
            console.log('\n💡 Tip: You may need to authenticate first');
            console.log('Try running: node connect-real-bank-account.js');
        }
    }
}

// Test environment variables
async function testEnvironmentVariables() {
    console.log('\n🔍 Testing Environment Variables...');
    
    try {
        const envResponse = await axios.get(`${BASE_URL}/api/test-env-direct`);
        console.log('✅ Environment test endpoint available');
        console.log('Environment info:', envResponse.data);
        
    } catch (error) {
        console.log('❌ Environment test failed:', error.response && error.response.data || error.message);
    }
}

async function runAllTests() {
    await testEnvironmentVariables();
    await testPlaidIntegration();
    
    console.log('\n🎯 Test Summary:');
    console.log('If you see ✅ marks above, the backend integration is working.');
    console.log('If you see ❌ marks, there may be configuration issues.');
    console.log('\nNext steps:');
    console.log('1. Test the mobile app Plaid integration');
    console.log('2. Check Android logs for any crash details');
    console.log('3. Verify Plaid SDK is properly included in the app');
}

runAllTests();