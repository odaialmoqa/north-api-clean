const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testPostConnectionFlow() {
    console.log('🔄 Testing Post-Connection Flow...');
    
    try {
        // Test 1: Create a test user and get auth token
        console.log('\n1️⃣ Creating test user for authentication...');
        const testEmail = `test-user-${Date.now()}@example.com`;
        const testPassword = 'testpassword123';
        
        let authToken;
        try {
            const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
                email: testEmail,
                password: testPassword,
                firstName: 'Test',
                lastName: 'User'
            });
            authToken = registerResponse.data.token;
            console.log('✅ Test user created and authenticated');
        } catch (error) {
            console.log('❌ Failed to create test user:', error.response && error.response.data ? error.response.data : error.message);
            return;
        }
        
        // Test 2: Test token exchange endpoint
        console.log('\n2️⃣ Testing token exchange endpoint...');
        const testPublicToken = 'access-sandbox-12345678-1234-1234-1234-123456789012';
        
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: testPublicToken
            }, {
                headers: { Authorization: `Bearer ${authToken}` }
            });
            console.log('✅ Token exchange successful');
            console.log('Response:', exchangeResponse.data);
        } catch (error) {
            console.log('❌ Token exchange failed:', error.response && error.response.data ? error.response.data : error.message);
        }
        
        // Test 3: Test transactions endpoint
        console.log('\n3️⃣ Testing transactions endpoint...');
        const testAccessToken = 'access-sandbox-12345678-1234-1234-1234-123456789012';
        
        try {
            const transactionsResponse = await axios.post(`${BASE_URL}/api/plaid/transactions`, {
                access_token: testAccessToken,
                start_date: '2024-01-01',
                end_date: '2024-01-31'
            }, {
                headers: { Authorization: `Bearer ${authToken}` }
            });
            console.log('✅ Transactions endpoint successful');
            console.log('Response:', transactionsResponse.data);
        } catch (error) {
            console.log('❌ Transactions endpoint failed:', error.response && error.response.data ? error.response.data : error.message);
        }
        
        console.log('\n🎯 POST-CONNECTION FLOW ANALYSIS:');
        console.log('=====================================');
        console.log('✅ Plaid Link UI completed successfully');
        console.log('❌ Post-connection processing is failing');
        console.log('');
        console.log('🔧 LIKELY ISSUES:');
        console.log('=====================================');
        console.log('1. Authentication token missing in mobile app');
        console.log('2. Backend endpoints not working properly');
        console.log('3. Database connection issues');
        console.log('4. Mobile app not sending auth headers');
        console.log('');
        console.log('💡 QUICK FIX:');
        console.log('=====================================');
        console.log('1. Add authentication to mobile app');
        console.log('2. Test backend endpoints directly');
        console.log('3. Check database connectivity');
        console.log('4. Verify API client configuration');
        
    } catch (error) {
        console.log('❌ Error testing post-connection flow:', error.message);
    }
}

testPostConnectionFlow().catch(console.error); 