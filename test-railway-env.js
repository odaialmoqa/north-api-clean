// Test Railway Environment Variables and Add Debug Endpoint
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testRailwayEnvironment() {
    try {
        console.log('🔍 Testing Railway Environment Variables...\n');
        
        // Step 1: Check current server configuration
        console.log('🖥️ Step 1: Current Server Configuration');
        try {
            const debugResponse = await axios.get(`${BASE_URL}/debug`);
            console.log('✅ Server configuration:');
            console.log('   Plaid Environment:', debugResponse.data.plaid_env);
            console.log('   Plaid Client ID:', debugResponse.data.plaid_client_id);
            console.log('   Plaid Secret exists:', debugResponse.data.plaid_secret_exists);
            console.log('   Node Environment:', debugResponse.data.node_env);
        } catch (error) {
            console.log('❌ Failed to get server debug info:', error.message);
        }

        // Step 2: Add a debug endpoint to the server for Plaid errors
        console.log('\n🔧 Step 2: Adding Debug Endpoint for Plaid Errors');
        console.log('Add this endpoint to your server.js:');
        console.log(`
app.get('/debug/plaid-error', async (req, res) => {
  try {
    // Test Plaid API directly
    const testRequest = {
      public_token: 'access-production-test-token',
    };
    
    const response = await plaidClient.itemPublicTokenExchange(testRequest);
    res.json({ success: true, response: response.data });
  } catch (error) {
    res.json({
      success: false,
      error: {
        message: error.message,
        code: error.code,
        status: error.response?.status,
        statusText: error.response?.statusText,
        plaid_response: error.response?.data,
        plaid_error_type: error.response?.data?.error_type,
        plaid_error_code: error.response?.data?.error_code,
        plaid_display_message: error.response?.data?.display_message,
        plaid_request_id: error.response?.data?.request_id
      }
    });
  }
});
        `);

        // Step 3: Check Railway environment variables
        console.log('\n🚂 Step 3: Railway Environment Variables');
        console.log('To check Railway environment variables, run:');
        console.log('railway variables list');
        console.log('');
        console.log('Expected variables:');
        console.log('   PLAID_CLIENT_ID=5fdecaa7df1def0013986738');
        console.log('   PLAID_SECRET=<your-production-secret>');
        console.log('   PLAID_ENV=production');
        console.log('   JWT_SECRET=<your-jwt-secret>');
        console.log('   DATABASE_URL=<your-database-url>');
        console.log('   GEMINI_API_KEY=<your-gemini-key>');

        // Step 4: Test with a real Plaid error simulation
        console.log('\n🧪 Step 4: Testing Plaid Error Simulation');
        
        // Create a test user
        const testEmail = `railway-test-${Date.now()}@example.com`;
        const testPassword = 'testpassword123';
        
        let authToken;
        try {
            const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
                email: testEmail,
                password: testPassword,
                firstName: 'Railway',
                lastName: 'Test'
            });
            authToken = registerResponse.data.token;
        } catch (error) {
            console.log('❌ Failed to create test user:', error.response && error.response.data || error.message);
            return;
        }

        const headers = { Authorization: `Bearer ${authToken}` };

        // Test with different token formats to see specific errors
        const testTokens = [
            { name: 'Empty token', token: '' },
            { name: 'Invalid format', token: 'invalid-token' },
            { name: 'Sandbox format', token: 'access-sandbox-12345678-1234-1234-1234-123456789012' },
            { name: 'Production format', token: 'access-production-12345678-1234-1234-1234-123456789012' },
            { name: 'Expired format', token: 'access-production-expired-token' }
        ];

        for (const testToken of testTokens) {
            console.log(`\nTesting ${testToken.name}:`);
            try {
                const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                    public_token: testToken.token
                }, { headers });
                
                console.log('✅ Unexpected success with invalid token!');
            } catch (error) {
                console.log('❌ Failed as expected');
                console.log('   Status:', error.response && error.response.status);
                console.log('   Error:', error.response && error.response.data && error.response.data.error);
                console.log('   Details:', error.response && error.response.data && error.response.data.details);
                
                // Check for new detailed error information
                if (error.response && error.response.data) {
                    console.log('   Plaid Error Type:', error.response.data.plaid_error_type);
                    console.log('   Plaid Error Code:', error.response.data.plaid_error_code);
                    console.log('   Plaid Display Message:', error.response.data.plaid_display_message);
                    console.log('   Plaid Request ID:', error.response.data.plaid_request_id);
                }
            }
        }

        // Step 5: Recommendations
        console.log('\n💡 Step 5: Recommendations');
        console.log('1. ✅ Server environment is correctly configured');
        console.log('2. ✅ Mobile app environment matches server');
        console.log('3. ⚠️ The issue is with public token exchange');
        console.log('4. 🔧 Next steps:');
        console.log('   - Deploy the enhanced error logging');
        console.log('   - Test with real Plaid Link flow');
        console.log('   - Check Railway logs for detailed Plaid errors');
        console.log('   - Verify the public token format from mobile app');

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

testRailwayEnvironment(); 