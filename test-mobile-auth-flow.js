// Test Mobile App Authentication Flow
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testMobileAuthFlow() {
    try {
        console.log('🔍 Testing Mobile App Authentication Flow...\n');
        
        // Step 1: Simulate mobile app registration/login
        console.log('📱 Step 1: Mobile App Authentication');
        const mobileEmail = `mobile-test-${Date.now()}@example.com`;
        const mobilePassword = 'mobilepassword123';
        
        let authToken;
        try {
            const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
                email: mobileEmail,
                password: mobilePassword,
                firstName: 'Mobile',
                lastName: 'User'
            });
            authToken = registerResponse.data.token;
            console.log('✅ Mobile user authenticated');
            console.log('Token preview:', authToken.substring(0, 20) + '...\n');
        } catch (error) {
            console.log('❌ Mobile authentication failed:', error.response && error.response.data || error.message);
            return;
        }

        const headers = { Authorization: `Bearer ${authToken}` };

        // Step 2: Create link token (mobile app does this)
        console.log('🔗 Step 2: Creating Link Token (Mobile App)');
        try {
            const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {}, { headers });
            console.log('✅ Link token created for mobile app');
            console.log('Token preview:', linkTokenResponse.data.link_token.substring(0, 20) + '...\n');
        } catch (error) {
            console.log('❌ Link token creation failed:', error.response && error.response.data || error.message);
            return;
        }

        // Step 3: Test exchange with authentication (simulate mobile app)
        console.log('🔄 Step 3: Testing Public Token Exchange (Mobile App)');
        
        // This simulates what happens when a user completes Plaid Link in the mobile app
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
            
            if (error.response && error.response.data && error.response.data.error) {
                console.log('\n🔍 Detailed Error Analysis:');
                console.log('Error type:', error.response.data.error);
                console.log('Error details:', error.response.data.details);
                console.log('Plaid configured:', error.response.data.plaid_configured);
            }
        }

        // Step 4: Test without authentication (should fail)
        console.log('\n🔒 Step 4: Testing Without Authentication (Should Fail)');
        try {
            const unauthenticatedResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: mockPublicToken
            });
            console.log('❌ This should have failed - authentication bypassed!');
        } catch (error) {
            if (error.response && error.response.status === 401) {
                console.log('✅ Correctly rejected unauthenticated request');
            } else {
                console.log('⚠️ Unexpected error for unauthenticated request:', error.response && error.response.status);
            }
        }

        console.log('\n💡 Analysis:');
        console.log('- If Step 3 fails with 500 error, the issue is in the Plaid API call');
        console.log('- If Step 3 fails with 401 error, there\'s an authentication issue');
        console.log('- If Step 3 succeeds, the issue is with real public tokens from mobile app');

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

testMobileAuthFlow(); 