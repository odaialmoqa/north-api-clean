// Test Environment Configuration Mismatches
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testEnvironmentMismatch() {
    try {
        console.log('🔍 Testing Environment Configuration Mismatches...\n');
        
        // Step 1: Check server environment configuration
        console.log('🖥️ Step 1: Server Environment Configuration');
        try {
            const debugResponse = await axios.get(`${BASE_URL}/debug`);
            console.log('✅ Server debug info retrieved');
            console.log('Server Plaid Environment:', debugResponse.data.plaid_env);
            console.log('Server Plaid Client ID:', debugResponse.data.plaid_client_id);
            console.log('Server Plaid Secret exists:', debugResponse.data.plaid_secret_exists);
            console.log('Server Node Environment:', debugResponse.data.node_env);
        } catch (error) {
            console.log('❌ Failed to get server debug info:', error.message);
        }

        // Step 2: Check mobile app configuration (from PlaidConfig.kt)
        console.log('\n📱 Step 2: Mobile App Configuration');
        console.log('Mobile App Plaid Environment: PRODUCTION');
        console.log('Mobile App Client ID: 5fdecaa7df1def0013986738');
        console.log('Mobile App Production Secret: 370ff905f8cafc934b6b1da256e729');
        console.log('Mobile App Base URL: https://production.plaid.com');

        // Step 3: Check for mismatches
        console.log('\n🔍 Step 3: Environment Mismatch Analysis');
        
        // Get server configuration
        let serverConfig;
        try {
            const debugResponse = await axios.get(`${BASE_URL}/debug`);
            serverConfig = debugResponse.data;
        } catch (error) {
            console.log('❌ Cannot get server config for comparison');
            return;
        }

        const mobileConfig = {
            environment: 'PRODUCTION',
            clientId: '5fdecaa7df1def0013986738',
            baseUrl: 'https://production.plaid.com'
        };

        console.log('\n📊 Configuration Comparison:');
        console.log('Server Plaid Environment:', serverConfig.plaid_env);
        console.log('Mobile App Environment:', mobileConfig.environment);
        
        if (serverConfig.plaid_env === 'production' && mobileConfig.environment === 'PRODUCTION') {
            console.log('✅ Environment match: Both using PRODUCTION');
        } else {
            console.log('❌ Environment mismatch detected!');
            console.log('   Server:', serverConfig.plaid_env);
            console.log('   Mobile:', mobileConfig.environment);
        }

        console.log('\nClient ID Comparison:');
        console.log('Server Client ID:', serverConfig.plaid_client_id);
        console.log('Mobile Client ID:', mobileConfig.clientId);
        
        if (serverConfig.plaid_client_id === mobileConfig.clientId) {
            console.log('✅ Client ID match');
        } else {
            console.log('❌ Client ID mismatch!');
        }

        // Step 4: Test link token creation with different environments
        console.log('\n🧪 Step 4: Testing Link Token Creation');
        
        // Create a test user
        const testEmail = `env-test-${Date.now()}@example.com`;
        const testPassword = 'testpassword123';
        
        let authToken;
        try {
            const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
                email: testEmail,
                password: testPassword,
                firstName: 'Env',
                lastName: 'Test'
            });
            authToken = registerResponse.data.token;
        } catch (error) {
            console.log('❌ Failed to create test user:', error.response && error.response.data || error.message);
            return;
        }

        const headers = { Authorization: `Bearer ${authToken}` };

        // Test link token creation
        try {
            const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {}, { headers });
            const linkToken = linkTokenResponse.data.link_token;
            
            console.log('✅ Link token created successfully');
            console.log('Token preview:', linkToken.substring(0, 20) + '...');
            
            // Check if it's a production or sandbox token
            if (linkToken.startsWith('link-production-')) {
                console.log('✅ Token is production format (matches server environment)');
            } else if (linkToken.startsWith('link-sandbox-')) {
                console.log('⚠️ Token is sandbox format (server might be in sandbox mode)');
            } else {
                console.log('❓ Token format unknown:', linkToken.substring(0, 20) + '...');
            }
        } catch (error) {
            console.log('❌ Link token creation failed:', error.response && error.response.data || error.message);
        }

        // Step 5: Recommendations
        console.log('\n💡 Step 5: Recommendations');
        
        if (serverConfig.plaid_env === 'production') {
            console.log('✅ Server is correctly configured for PRODUCTION');
            console.log('✅ Mobile app should work with real bank connections');
            console.log('⚠️ The 500 error is likely due to:');
            console.log('   - Invalid public token format');
            console.log('   - Token expiration');
            console.log('   - Network issues');
            console.log('   - Plaid API rate limiting');
        } else if (serverConfig.plaid_env === 'sandbox') {
            console.log('⚠️ Server is in SANDBOX mode but mobile app expects PRODUCTION');
            console.log('🔧 Fix: Update server PLAID_ENV to "production"');
        } else {
            console.log('❓ Unknown server environment:', serverConfig.plaid_env);
        }

        console.log('\n🔧 Next Steps:');
        console.log('1. Check Railway environment variables');
        console.log('2. Verify PLAID_ENV is set to "production"');
        console.log('3. Test with real Plaid Link flow');
        console.log('4. Check server logs for detailed Plaid errors');

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

testEnvironmentMismatch(); 