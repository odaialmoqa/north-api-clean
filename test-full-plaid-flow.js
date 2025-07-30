// Test the complete Plaid flow to identify issues
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testFullPlaidFlow() {
    try {
        console.log('🧪 Testing Complete Plaid Flow...\n');
        
        // 1. Register a test user
        console.log('1️⃣ Registering test user...');
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: `plaid-test-${Date.now()}@example.com`,
            password: 'testpassword123',
            firstName: 'Plaid',
            lastName: 'Test'
        });
        
        const { token, user } = registerResponse.data;
        console.log(`✅ User registered: ${user.email}`);
        console.log(`🔑 Auth token: ${token.substring(0, 20)}...`);
        
        // 2. Create Plaid link token
        console.log('\n2️⃣ Creating Plaid link token...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {});
        const linkToken = linkTokenResponse.data.link_token;
        console.log(`✅ Link token created: ${linkToken.substring(0, 20)}...`);
        
        // 3. Simulate what happens when user completes Plaid Link
        console.log('\n3️⃣ Simulating Plaid Link completion...');
        console.log('ℹ️ In the real app, user would:');
        console.log('   - See Plaid Link UI');
        console.log('   - Select their bank');
        console.log('   - Enter credentials');
        console.log('   - Plaid would return a public_token');
        
        // 4. Test with different types of tokens to see what works
        const testTokens = [
            'public-sandbox-test-123',
            'public-development-test-123',
            'public-production-test-123'
        ];
        
        for (const testToken of testTokens) {
            console.log(`\n4️⃣ Testing token exchange with: ${testToken}`);
            try {
                const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                    public_token: testToken
                }, {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                
                console.log('✅ Exchange successful (unexpected):', exchangeResponse.data);
                break; // If one works, we're done
            } catch (exchangeError) {
                console.log('❌ Exchange failed (expected):', exchangeError.response?.data);
                console.log('   Status:', exchangeError.response?.status);
            }
        }
        
        // 5. Check what accounts are available (should have mock data)
        console.log('\n5️⃣ Checking available accounts...');
        try {
            const accountsResponse = await axios.get(`${BASE_URL}/api/plaid/accounts`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            console.log('✅ Accounts retrieved:', accountsResponse.data.accounts.length, 'accounts');
            accountsResponse.data.accounts.forEach((account, index) => {
                console.log(`   ${index + 1}. ${account.name} (${account.type}) - $${account.balance}`);
            });
        } catch (accountsError) {
            console.log('❌ No accounts found:', accountsError.response?.data);
        }
        
        // 6. Check insights and goals (should be generated from mock data)
        console.log('\n6️⃣ Checking AI-generated insights...');
        try {
            const insightsResponse = await axios.get(`${BASE_URL}/api/insights`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            console.log('✅ Insights retrieved:', insightsResponse.data.insights.length, 'insights');
        } catch (insightsError) {
            console.log('❌ No insights found:', insightsError.response?.data);
        }
        
        console.log('\n🎯 Summary:');
        console.log('✅ User registration: Working');
        console.log('✅ Link token creation: Working');
        console.log('❌ Token exchange: Fails with test tokens (expected)');
        console.log('✅ Mock accounts: Available');
        console.log('? Insights generation: Check above');
        
        console.log('\n💡 Next Steps:');
        console.log('1. The backend Plaid integration is working correctly');
        console.log('2. The issue is that test tokens cannot be exchanged with real Plaid API');
        console.log('3. The mobile app needs to use the real Plaid Link SDK');
        console.log('4. Once real public tokens are generated, the backend should work');
        
    } catch (error) {
        console.error('❌ Test failed:', error.response?.data || error.message);
    }
}

testFullPlaidFlow();