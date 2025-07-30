// Test script to verify Plaid credentials are working
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testAfterPlaidSetup() {
    try {
        console.log('🧪 Testing After Plaid Setup...\n');
        
        // 1. Check backend configuration
        console.log('1️⃣ Checking backend Plaid configuration...');
        const debugResponse = await axios.get(`${BASE_URL}/debug`);
        console.log('🔧 Plaid config:', {
            client_id: debugResponse.data.plaid_client_id ? '✅ SET' : '❌ MISSING',
            secret: debugResponse.data.plaid_secret_exists ? '✅ SET' : '❌ MISSING',
            env: debugResponse.data.plaid_env || '❌ NOT SET'
        });
        
        if (!debugResponse.data.plaid_client_id || !debugResponse.data.plaid_secret_exists) {
            console.log('\n❌ Plaid credentials still missing!');
            console.log('Please add PLAID_CLIENT_ID and PLAID_SECRET to Railway environment variables.');
            return;
        }
        
        // 2. Test link token creation
        console.log('\n2️⃣ Testing link token creation...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {});
        console.log('✅ Link token created successfully');
        console.log(`🔗 Token: ${linkTokenResponse.data.link_token.substring(0, 20)}...`);
        
        // 3. Register test user
        console.log('\n3️⃣ Registering test user...');
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: `test-${Date.now()}@example.com`,
            password: 'testpassword123',
            firstName: 'Test',
            lastName: 'User'
        });
        
        const { token } = registerResponse.data;
        console.log('✅ User registered');
        
        // 4. Test token exchange (should still fail with test token, but with better error)
        console.log('\n4️⃣ Testing token exchange...');
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: 'public-sandbox-test-123'
            }, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            
            console.log('✅ Exchange successful (unexpected):', exchangeResponse.data);
        } catch (exchangeError) {
            console.log('❌ Exchange failed (expected with test token)');
            console.log('Status:', exchangeError.response?.status);
            console.log('Error:', exchangeError.response?.data);
            
            // If it's a 400 error instead of 500, that means Plaid credentials are working
            if (exchangeError.response?.status === 400) {
                console.log('✅ Good! 400 error means Plaid API is reachable (credentials work)');
            } else if (exchangeError.response?.status === 500) {
                console.log('❌ Still getting 500 error - credentials might be wrong');
            }
        }
        
        console.log('\n🎯 Next Step:');
        console.log('If you see "✅ Good! 400 error", then try the mobile app again!');
        console.log('The real Plaid public token from your phone should now work.');
        
    } catch (error) {
        console.error('❌ Test failed:', error.response?.data || error.message);
    }
}

testAfterPlaidSetup();