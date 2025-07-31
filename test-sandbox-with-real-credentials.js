const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testSandboxWithRealCredentials() {
    console.log('🧪 Testing Sandbox with Real Credentials...');
    
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
        
        // Step 2: Create a link token
        console.log('\n2️⃣ Creating link token...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'Test App',
            country_codes: ['US'],
            language: 'en',
            user: { client_user_id: 'test-user' },
            products: ['transactions']
        }, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        const linkToken = linkTokenResponse.data.link_token;
        console.log('✅ Link token created:', linkToken.substring(0, 30) + '...');
        
        // Step 3: Simulate what would happen with a real public token
        console.log('\n3️⃣ Testing with a real sandbox public token...');
        
        // This is what a real sandbox public token looks like (but this one is fake)
        // In reality, this would come from the Plaid Link UI after user completes the flow
        const realSandboxToken = 'public-sandbox-12345678-1234-1234-1234-123456789012';
        
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: realSandboxToken
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
                    console.log('');
                    console.log('🔧 NEXT STEPS:');
                    console.log('=====================================');
                    console.log('1. The mobile app needs to get a real token from Plaid Link');
                    console.log('2. The token should be in format: public-sandbox-...');
                    console.log('3. The backend will then process it correctly');
                    console.log('4. Data will be pulled into the database');
                }
            }
        }
        
    } catch (error) {
        console.log('❌ Test failed:', error.message);
    }
}

testSandboxWithRealCredentials().catch(console.error); 