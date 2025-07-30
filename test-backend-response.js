const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testBackendResponse() {
    try {
        // Register a test user
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: `test-${Date.now()}@example.com`,
            password: 'testpassword123',
            firstName: 'Test',
            lastName: 'User'
        });
        
        const { token } = registerResponse.data;
        console.log('✅ User registered with token:', token.substring(0, 20) + '...');
        
        // Try to exchange a test token to see the error format
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: 'test-token-123'
            }, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            console.log('✅ Exchange successful:', exchangeResponse.data);
        } catch (exchangeError) {
            console.log('❌ Exchange failed (expected):', exchangeError.response?.data);
            console.log('Status:', exchangeError.response?.status);
        }
        
    } catch (error) {
        console.error('Test failed:', error.response?.data || error.message);
    }
}

testBackendResponse();