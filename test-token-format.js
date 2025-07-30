// Test Token Format Analysis
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testTokenFormat() {
    try {
        console.log('🔍 Testing Token Format Analysis...\n');
        
        // Step 1: Create a test user
        console.log('📝 Step 1: Creating test user...');
        const testEmail = `token-test-${Date.now()}@example.com`;
        const testPassword = 'testpassword123';
        
        let authToken;
        try {
            const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
                email: testEmail,
                password: testPassword,
                firstName: 'Token',
                lastName: 'Test'
            });
            authToken = registerResponse.data.token;
            console.log('✅ Test user created and authenticated\n');
        } catch (error) {
            console.log('❌ Failed to create test user:', error.response && error.response.data || error.message);
            return;
        }

        const headers = { Authorization: `Bearer ${authToken}` };

        // Step 2: Test different token formats
        console.log('🧪 Step 2: Testing Different Token Formats');
        
        const testTokens = [
            { name: 'Correct format (public-)', token: 'public-production-12345678-1234-1234-1234-123456789012' },
            { name: 'Wrong format (access-)', token: 'access-production-12345678-1234-1234-1234-123456789012' },
            { name: 'Sandbox format', token: 'public-sandbox-12345678-1234-1234-1234-123456789012' },
            { name: 'Development format', token: 'public-development-12345678-1234-1234-1234-123456789012' },
            { name: 'Invalid format', token: 'invalid-token-format' },
            { name: 'Empty token', token: '' }
        ];

        for (const testToken of testTokens) {
            console.log(`\nTesting ${testToken.name}:`);
            console.log(`Token: ${testToken.token}`);
            
            try {
                const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                    public_token: testToken.token
                }, { headers });
                
                console.log('✅ SUCCESS! This format works');
                console.log('Response:', exchangeResponse.data);
            } catch (error) {
                console.log('❌ Failed as expected');
                console.log('   Status:', error.response && error.response.status);
                console.log('   Error:', error.response && error.response.data && error.response.data.error);
                console.log('   Details:', error.response && error.response.data && error.response.data.details);
                
                // Check for detailed Plaid error information
                if (error.response && error.response.data) {
                    console.log('   Plaid Error Type:', error.response.data.plaid_error_type);
                    console.log('   Plaid Error Code:', error.response.data.plaid_error_code);
                    console.log('   Plaid Display Message:', error.response.data.plaid_display_message);
                    console.log('   Plaid Request ID:', error.response.data.plaid_request_id);
                }
            }
        }

        // Step 3: Analysis
        console.log('\n💡 Step 3: Token Format Analysis');
        console.log('Based on the test results:');
        console.log('- Tokens starting with "public-" should work');
        console.log('- Tokens starting with "access-" will fail');
        console.log('- The mobile app should be sending "public-" tokens');
        console.log('- If the mobile app is sending "access-" tokens, that\'s the issue');

        // Step 4: Recommendations
        console.log('\n🔧 Step 4: Recommendations');
        console.log('1. Check the mobile app logs to see what token format is being sent');
        console.log('2. Verify the Plaid SDK version in the mobile app');
        console.log('3. Ensure the mobile app is using the correct Plaid environment');
        console.log('4. Test with a real Plaid Link flow to see the actual token format');

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

testTokenFormat(); 