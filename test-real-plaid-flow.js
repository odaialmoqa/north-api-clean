// Test Real Plaid Link Flow with Sandbox Credentials
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

// Plaid Sandbox Test Credentials (these are public test credentials)
const SANDBOX_TEST_CREDENTIALS = {
    username: 'user_good',
    password: 'pass_good'
};

async function testRealPlaidFlow() {
    try {
        console.log('🔍 Testing Real Plaid Link Flow with Sandbox...\n');
        
        // Step 1: Create a test user and get auth token
        console.log('📝 Step 1: Creating test user...');
        const testEmail = `real-plaid-test-${Date.now()}@example.com`;
        const testPassword = 'testpassword123';
        
        let authToken;
        try {
            const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
                email: testEmail,
                password: testPassword,
                firstName: 'Real',
                lastName: 'Plaid'
            });
            authToken = registerResponse.data.token;
            console.log('✅ Test user created and authenticated\n');
        } catch (error) {
            console.log('❌ Failed to create test user:', error.response && error.response.data || error.message);
            return;
        }

        const headers = { Authorization: `Bearer ${authToken}` };

        // Step 2: Create link token
        console.log('🔗 Step 2: Creating link token...');
        let linkToken;
        try {
            const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {}, { headers });
            linkToken = linkTokenResponse.data.link_token;
            console.log('✅ Link token created successfully');
            console.log('Token preview:', linkToken.substring(0, 20) + '...\n');
        } catch (error) {
            console.log('❌ Link token creation failed:', error.response && error.response.data || error.message);
            return;
        }

        // Step 3: Simulate Plaid Link completion with sandbox credentials
        console.log('🏦 Step 3: Simulating Plaid Link completion...');
        console.log('Using sandbox test credentials:', SANDBOX_TEST_CREDENTIALS);
        
        // In a real app, this would be done by the Plaid Link SDK
        // For testing, we'll simulate what happens when a user completes the flow
        console.log('📱 User would now complete Plaid Link with these credentials');
        console.log('   Username: user_good');
        console.log('   Password: pass_good');
        console.log('   This would generate a real public token\n');

        // Step 4: Test with a sandbox public token (this should work)
        console.log('🔄 Step 4: Testing with sandbox public token...');
        
        // This is a sandbox test token that should work
        const sandboxPublicToken = 'access-sandbox-12345678-1234-1234-1234-123456789012';
        
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: sandboxPublicToken
            }, { headers });
            
            console.log('✅ Public token exchange successful!');
            console.log('Response:', exchangeResponse.data);
        } catch (error) {
            console.log('❌ Public token exchange failed:');
            console.log('Status:', error.response && error.response.status);
            console.log('Error data:', error.response && error.response.data);
            console.log('Error message:', error.message);
            
            if (error.response && error.response.data && error.response.data.error) {
                console.log('\n🔍 Detailed Error Analysis:');
                console.log('Error type:', error.response.data.error);
                console.log('Error details:', error.response.data.details);
                console.log('Plaid error type:', error.response.data.plaid_error_type);
                console.log('Plaid error code:', error.response.data.plaid_error_code);
                console.log('Plaid display message:', error.response.data.plaid_display_message);
            }
        }

        // Step 5: Test with production token format (should fail)
        console.log('\n🔄 Step 5: Testing with production token format...');
        const productionPublicToken = 'access-production-12345678-1234-1234-1234-123456789012';
        
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: productionPublicToken
            }, { headers });
            
            console.log('✅ Production token exchange successful!');
            console.log('Response:', exchangeResponse.data);
        } catch (error) {
            console.log('❌ Production token exchange failed (expected):');
            console.log('Status:', error.response && error.response.status);
            console.log('Error data:', error.response && error.response.data);
            
            if (error.response && error.response.data && error.response.data.error) {
                console.log('\n🔍 Production Token Error Analysis:');
                console.log('Error type:', error.response.data.error);
                console.log('Error details:', error.response.data.details);
                console.log('Plaid error type:', error.response.data.plaid_error_type);
                console.log('Plaid error code:', error.response.data.plaid_error_code);
                console.log('Plaid display message:', error.response.data.plaid_display_message);
            }
        }

        console.log('\n💡 Analysis:');
        console.log('- If Step 4 succeeds, the server can handle sandbox tokens');
        console.log('- If Step 4 fails, there might be an environment mismatch');
        console.log('- If Step 5 fails (expected), the server is correctly configured for production');
        console.log('- The real issue is likely that the mobile app needs to use the correct Plaid environment');

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

testRealPlaidFlow(); 