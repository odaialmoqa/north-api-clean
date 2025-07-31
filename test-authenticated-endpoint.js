const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testAuthenticatedEndpoint() {
    console.log('🔐 Testing Authenticated Endpoint...');
    
    try {
        // Step 1: Create a test user and get auth token
        console.log('\n1️⃣ Creating test user...');
        const testEmail = `test-user-${Date.now()}@example.com`;
        const testPassword = 'testpassword123';
        
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: testEmail,
            password: testPassword,
            firstName: 'Test',
            lastName: 'User'
        });
        
        const authToken = registerResponse.data.token;
        console.log('✅ Test user created, auth token received');
        
        // Step 2: Test the exact endpoint the mobile app calls
        console.log('\n2️⃣ Testing /api/plaid/exchange-public-token with auth...');
        
        const testPublicToken = 'access-sandbox-12345678-1234-1234-1234-123456789012';
        
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: testPublicToken
            }, {
                headers: { 
                    'Authorization': `Bearer ${authToken}`,
                    'Content-Type': 'application/json'
                }
            });
            
            console.log('✅ Token exchange successful!');
            console.log('Response:', exchangeResponse.data);
            
        } catch (error) {
            console.log('❌ Token exchange failed');
            console.log('Status:', error.response && error.response.status);
            console.log('Status Text:', error.response && error.response.statusText);
            console.log('Error Data:', error.response && error.response.data);
            console.log('Error Message:', error.message);
            
            if (error.response && error.response.status === 502) {
                console.log('\n🎯 502 ERROR ANALYSIS:');
                console.log('=====================================');
                console.log('The backend is crashing when processing this request');
                console.log('This could be due to:');
                console.log('1. Database connection failure');
                console.log('2. Plaid API call failure');
                console.log('3. Memory/CPU limits on Railway');
                console.log('4. Environment variable issues');
                console.log('');
                console.log('💡 IMMEDIATE FIX:');
                console.log('=====================================');
                console.log('1. Check Railway logs for crash details');
                console.log('2. Restart the Railway service');
                console.log('3. Check database connectivity');
                console.log('4. Verify Plaid credentials');
            }
        }
        
    } catch (error) {
        console.log('❌ Test failed:', error.message);
    }
}

testAuthenticatedEndpoint().catch(console.error); 