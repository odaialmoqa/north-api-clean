const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testValidSandboxToken() {
    console.log('🔐 Testing with Valid Sandbox Token...');
    
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
        
        // Step 2: Test with a properly formatted sandbox token
        console.log('\n2️⃣ Testing with valid sandbox token format...');
        
        // This is a properly formatted sandbox token (but still invalid for actual exchange)
        const validSandboxToken = 'public-sandbox-12345678-1234-1234-1234-123456789012';
        
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: validSandboxToken
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
            console.log('Error Data:', error.response && error.response.data);
            
            if (error.response && error.response.data && error.response.data.details) {
                const details = error.response.data.details;
                console.log('\n🎯 PLAID ERROR ANALYSIS:');
                console.log('=====================================');
                console.log('Error Code:', details.error_code);
                console.log('Error Type:', details.error_type);
                console.log('Error Message:', details.error_message);
                console.log('Request ID:', details.request_id);
                
                if (details.error_code === 'INVALID_PUBLIC_TOKEN') {
                    console.log('\n💡 SOLUTION:');
                    console.log('=====================================');
                    console.log('The backend is working correctly!');
                    console.log('The issue is that we need a REAL token from Plaid Link');
                    console.log('The mobile app should get a valid token from the Plaid UI');
                    console.log('This test confirms the backend can process requests');
                }
            }
        }
        
    } catch (error) {
        console.log('❌ Test failed:', error.message);
    }
}

testValidSandboxToken().catch(console.error); 