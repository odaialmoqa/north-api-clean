const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testSandboxBanks() {
    console.log('🏦 Testing Different Banks in Sandbox Mode...');
    
    try {
        // Create sandbox link token
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'North Financial',
            country_codes: ['US', 'CA'],
            language: 'en',
            user: { client_user_id: 'test-user' },
            products: ['transactions']
        });
        
        console.log('✅ Sandbox link token created');
        
        console.log('\n🎯 RECOMMENDED SANDBOX BANKS:');
        console.log('=====================================');
        console.log('');
        console.log('🇺🇸 US BANKS (Try in this order):');
        console.log('  1. Wells Fargo (most reliable in sandbox)');
        console.log('  2. Capital One');
        console.log('  3. American Express');
        console.log('  4. Bank of America');
        console.log('  5. Chase Bank (requires registration in production)');
        console.log('');
        console.log('🇨🇦 CANADIAN BANKS:');
        console.log('  1. RBC Royal Bank');
        console.log('  2. TD Canada Trust');
        console.log('  3. Scotiabank');
        console.log('');
        console.log('🔑 TEST CREDENTIALS:');
        console.log('=====================================');
        console.log('Username: user_good');
        console.log('Password: pass_good');
        console.log('');
        console.log('💡 WHY CHASE FAILED:');
        console.log('=====================================');
        console.log('• Chase requires special registration in production');
        console.log('• Your Plaid account isn\'t registered for Chase yet');
        console.log('• This is normal for production mode');
        console.log('• Sandbox mode should work with other banks');
        console.log('');
        console.log('🚀 NEXT STEPS:');
        console.log('=====================================');
        console.log('1. Try Wells Fargo first (most reliable)');
        console.log('2. Use test credentials: user_good / pass_good');
        console.log('3. If Wells Fargo works, the flow is working!');
        console.log('4. Then try other banks to test variety');
        
    } catch (error) {
        console.log('❌ Error:', error.response && error.response.data ? error.response.data : error.message);
    }
}

testSandboxBanks().catch(console.error); 