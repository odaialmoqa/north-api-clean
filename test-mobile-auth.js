// Test script to verify mobile app authentication flow
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testMobileAuth() {
    try {
        console.log('🧪 Testing Mobile App Authentication Flow...\n');
        
        // 1. Register a test user (simulating mobile app registration)
        console.log('1️⃣ Registering test user...');
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: `mobile-test-${Date.now()}@example.com`,
            password: 'testpassword123',
            firstName: 'Mobile',
            lastName: 'User'
        });
        
        const { token, user } = registerResponse.data;
        console.log(`✅ User registered: ${user.email}`);
        console.log(`🔑 Auth token: ${token.substring(0, 20)}...`);
        
        // 2. Test token validation by getting goals (requires auth)
        console.log('\n2️⃣ Testing token validation...');
        const goalsResponse = await axios.get(`${BASE_URL}/api/goals`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        console.log('✅ Token validation successful');
        console.log('🎯 Goals response:', goalsResponse.data.length, 'goals found');
        
        // 3. Test Plaid link token creation (should work)
        console.log('\n3️⃣ Testing Plaid link token creation...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {});
        console.log('✅ Link token created successfully');
        console.log(`🔗 Link token: ${linkTokenResponse.data.link_token.substring(0, 20)}...`);
        
        // 4. Test Plaid token exchange with test token (should fail gracefully)
        console.log('\n4️⃣ Testing Plaid token exchange with test token...');
        try {
            const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                public_token: 'public-test-token-123'
            }, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            console.log('✅ Exchange successful (unexpected):', exchangeResponse.data);
        } catch (exchangeError) {
            console.log('❌ Exchange failed (expected with test token)');
            console.log('📝 Error response:', exchangeError.response?.data);
            console.log('📝 Status code:', exchangeError.response?.status);
        }
        
        console.log('\n🎯 Summary:');
        console.log('✅ User registration: Working');
        console.log('✅ Token validation: Working');
        console.log('✅ Link token creation: Working');
        console.log('❌ Token exchange: Fails with test tokens (expected)');
        console.log('\n💡 Next step: Test with real Plaid Link flow in mobile app');
        
    } catch (error) {
        console.error('❌ Test failed:', error.response?.data || error.message);
    }
}

testMobileAuth();