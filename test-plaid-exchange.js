// Test Plaid Public Token Exchange
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testPlaidExchange() {
    try {
        console.log('🔍 Testing Plaid Public Token Exchange...\n');
        
        // Step 1: Create a test user and get auth token
        console.log('📝 Step 1: Creating test user...');
        const testEmail = `test-${Date.now()}@example.com`;
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
            console.log('✅ Test user created and authenticated\n');
        } catch (error) {
            console.log('❌ Failed to create test user:', error.response && error.response.data || error.message);
            return;
        }

        const headers = { Authorization: `Bearer ${authToken}` };

        // Step 2: Create link token (this works)
        console.log('🔗 Step 2: Creating link token...');
        try {
            const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {}, { headers });
            console.log('✅ Link token created successfully');
            console.log('Token preview:', linkTokenResponse.data.link_token.substring(0, 20) + '...\n');
        } catch (error) {
            console.log('❌ Link token creation failed:', error.response && error.response.data || error.message);
            return;
        }

        // Step 3: Test public token exchange with a mock token
        console.log('🔄 Step 3: Testing public token exchange...');
        const mockPublicToken = 'access-production-12345678-1234-1234-1234-123456789012';
        
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: mockPublicToken
            }, { headers });
            
            console.log('✅ Public token exchange successful!');
            console.log('Response:', exchangeResponse.data);
        } catch (error) {
            console.log('❌ Public token exchange failed:');
            console.log('Status:', error.response && error.response.status);
            console.log('Error data:', error.response && error.response.data);
            console.log('Error message:', error.message);
            
            // Check if it's a Plaid API error
            if (error.response && error.response.data && error.response.data.error) {
                console.log('\n🔍 Plaid API Error Details:');
                console.log('Error type:', error.response.data.error);
                console.log('Error details:', error.response.data.details);
            }
        }

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

testPlaidExchange(); 