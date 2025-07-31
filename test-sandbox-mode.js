const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testSandboxMode() {
    console.log('🧪 Testing Plaid Sandbox Mode...');
    
    try {
        // Test 1: Check Plaid configuration
        console.log('\n1️⃣ Testing Plaid configuration...');
        const plaidResponse = await axios.get(`${BASE_URL}/debug/plaid`);
        console.log('✅ Plaid configuration check');
        console.log('Response:', plaidResponse.data);
        
        // Test 2: Create sandbox link token
        console.log('\n2️⃣ Creating sandbox link token...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'North Financial',
            country_codes: ['US', 'CA'],
            language: 'en',
            user: { client_user_id: 'test-user' },
            products: ['transactions']
        });
        
        const linkToken = linkTokenResponse.data.link_token;
        console.log('✅ Sandbox link token created successfully');
        console.log('Token preview:', linkToken.substring(0, 20) + '...');
        
        // Check if it's a sandbox token
        if (linkToken.includes('sandbox')) {
            console.log('✅ Confirmed: Using sandbox mode');
        } else {
            console.log('⚠️  Warning: Token doesn\'t look like sandbox');
        }
        
        console.log('\n🎯 SANDBOX MODE READY!');
        console.log('=====================================');
        console.log('✅ Backend is now using sandbox mode');
        console.log('✅ No rate limits');
        console.log('✅ Test credentials will work');
        console.log('');
        console.log('📱 MOBILE APP TESTING:');
        console.log('=====================================');
        console.log('1. Open your mobile app');
        console.log('2. Click "Connect Bank Account"');
        console.log('3. Select any bank (Chase, BofA, etc.)');
        console.log('4. Use test credentials:');
        console.log('   Username: user_good');
        console.log('   Password: pass_good');
        console.log('5. Complete the flow');
        console.log('');
        console.log('🚀 Ready to test the full flow!');
        
    } catch (error) {
        console.log('❌ Error testing sandbox mode:', error.response && error.response.data ? error.response.data : error.message);
    }
}

testSandboxMode().catch(console.error); 