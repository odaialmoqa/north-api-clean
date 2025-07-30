const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testPlaidIntegration() {
    console.log('🧪 Testing Plaid Integration...\n');
    
    try {
        // Step 1: Register a test user
        console.log('1️⃣ Registering test user...');
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: `test-${Date.now()}@example.com`,
            password: 'testpassword123',
            firstName: 'Test',
            lastName: 'User'
        });
        
        const { token, user } = registerResponse.data;
        console.log(`✅ User registered: ${user.email}`);
        console.log(`🔑 Auth token: ${token.substring(0, 20)}...`);
        
        // Step 2: Create link token
        console.log('\n2️⃣ Creating Plaid link token...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`);
        const { link_token } = linkTokenResponse.data;
        console.log(`🔗 Link token: ${link_token.substring(0, 20)}...`);
        
        // Step 3: Simulate public token (normally comes from Plaid UI)
        console.log('\n3️⃣ Simulating public token exchange...');
        const publicToken = 'public-sandbox-test-' + Date.now();
        console.log(`📱 Simulated public token: ${publicToken}`);
        
        // Step 4: Exchange public token
        console.log('\n4️⃣ Exchanging public token...');
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: publicToken
            }, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            console.log('✅ Token exchange successful!');
            console.log('📊 Response:', JSON.stringify(exchangeResponse.data, null, 2));
            
        } catch (exchangeError) {
            console.log('❌ Token exchange failed (expected with test token)');
            console.log('📝 Error:', exchangeError.response?.data || exchangeError.message);
        }
        
        // Step 5: Check stored accounts
        console.log('\n5️⃣ Checking stored accounts...');
        try {
            const accountsResponse = await axios.get(`${BASE_URL}/api/plaid/accounts`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            console.log('✅ Accounts retrieved!');
            console.log('📊 Accounts:', JSON.stringify(accountsResponse.data, null, 2));
            
        } catch (accountsError) {
            console.log('❌ No accounts found (expected if exchange failed)');
            console.log('📝 Error:', accountsError.response?.data || accountsError.message);
        }
        
    } catch (error) {
        console.error('❌ Test failed:', error.response?.data || error.message);
    }
}

testPlaidIntegration();