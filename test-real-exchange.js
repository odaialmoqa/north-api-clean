// Test with a realistic public token to see the actual error
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testRealExchange() {
    try {
        console.log('🔍 Testing Real Token Exchange...\n');
        
        // 1. Register a user and get auth token
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: `real-test-${Date.now()}@example.com`,
            password: 'testpassword123',
            firstName: 'Real',
            lastName: 'Test'
        });
        
        const { token } = registerResponse.data;
        console.log('✅ User registered');
        
        // 2. Create a link token to verify Plaid is working
        const linkResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`);
        console.log('✅ Link token created:', linkResponse.data.link_token.substring(0, 20) + '...');
        
        // 3. Test with a realistic production public token format
        // This is what Plaid actually returns in production
        const realisticToken = 'public-production-' + Math.random().toString(36).substring(2, 15) + '-' + Math.random().toString(36).substring(2, 15);
        
        console.log('\n🧪 Testing exchange with realistic token format...');
        console.log('Token:', realisticToken);
        
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: realisticToken
            }, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                timeout: 15000 // 15 second timeout
            });
            
            console.log('✅ Exchange successful:', exchangeResponse.data);
            
        } catch (exchangeError) {
            console.log('❌ Exchange failed:');
            console.log('Status:', exchangeError.response?.status);
            console.log('Headers:', exchangeError.response?.headers);
            console.log('Data:', exchangeError.response?.data);
            
            // Check if it's a timeout
            if (exchangeError.code === 'ECONNABORTED') {
                console.log('\n⏰ Request timed out - backend is taking too long');
                console.log('This suggests the backend is trying to call Plaid API but failing');
            }
            
            // Check if it's a specific Plaid error
            if (exchangeError.response?.data?.error_code) {
                console.log('\n🔍 Plaid API Error Details:');
                console.log('Error Code:', exchangeError.response.data.error_code);
                console.log('Error Type:', exchangeError.response.data.error_type);
                console.log('Error Message:', exchangeError.response.data.error_message);
            }
        }
        
        // 4. Let's also check what the backend debug endpoint shows now
        console.log('\n🔧 Current backend config:');
        const debugResponse = await axios.get(`${BASE_URL}/debug`);
        console.log('Plaid Client ID:', debugResponse.data.plaid_client_id || 'MISSING');
        console.log('Plaid Secret exists:', debugResponse.data.plaid_secret_exists || false);
        console.log('Plaid Environment:', debugResponse.data.plaid_env || 'NOT SET');
        
    } catch (error) {
        console.error('❌ Test failed:', error.message);
        if (error.response) {
            console.log('Response status:', error.response.status);
            console.log('Response data:', error.response.data);
        }
    }
}

testRealExchange();