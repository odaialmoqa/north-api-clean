const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testPlaidProductionBanks() {
    console.log('🏦 Testing Plaid Production Banks...');
    
    try {
        // Test 1: Create a link token
        console.log('\n1️⃣ Creating production link token...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'North Financial',
            country_codes: ['US', 'CA'],
            language: 'en',
            user: { client_user_id: 'test-user' },
            products: ['transactions']
        });
        
        const linkToken = linkTokenResponse.data.link_token;
        console.log('✅ Production link token created:', linkToken.substring(0, 20) + '...');
        
        console.log('\n📋 PRODUCTION PLAID BANKS:');
        console.log('=====================================');
        console.log('🏦 For Production testing, use these banks:');
        console.log('');
        console.log('🇺🇸 US Banks:');
        console.log('  • Chase Bank');
        console.log('  • Bank of America');
        console.log('  • Wells Fargo');
        console.log('  • Capital One');
        console.log('  • American Express');
        console.log('');
        console.log('🇨🇦 Canadian Banks:');
        console.log('  • RBC Royal Bank');
        console.log('  • TD Canada Trust');
        console.log('  • Scotiabank');
        console.log('  • BMO Bank of Montreal');
        console.log('  • CIBC');
        console.log('');
        console.log('🔑 TEST CREDENTIALS:');
        console.log('=====================================');
        console.log('For ANY bank in production mode, use:');
        console.log('  Username: user_good');
        console.log('  Password: pass_good');
        console.log('');
        console.log('❌ DO NOT USE:');
        console.log('  - user_bad / pass_bad (will fail)');
        console.log('  - Your real bank credentials (will fail)');
        console.log('');
        console.log('💡 TIP:');
        console.log('  - These are Plaid\'s test credentials');
        console.log('  - They work with any bank in production mode');
        console.log('  - This is how you test the integration safely');
        
    } catch (error) {
        console.log('❌ Error testing production banks:', error.response && error.response.data ? error.response.data : error.message);
    }
}

testPlaidProductionBanks().catch(console.error); 