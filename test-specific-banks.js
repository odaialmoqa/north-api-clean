const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testSpecificBanks() {
    console.log('🏦 Testing Specific Banks with Production Plaid...');
    
    try {
        // Create a link token
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'North Financial',
            country_codes: ['US', 'CA'],
            language: 'en',
            user: { client_user_id: 'test-user' },
            products: ['transactions']
        });
        
        console.log('✅ Link token created successfully');
        
        console.log('\n🎯 RECOMMENDED BANKS TO TEST:');
        console.log('=====================================');
        console.log('');
        console.log('🇺🇸 US BANKS (Most Reliable):');
        console.log('  1. Chase Bank');
        console.log('  2. Bank of America');
        console.log('  3. Wells Fargo');
        console.log('  4. Capital One');
        console.log('  5. American Express');
        console.log('');
        console.log('🇨🇦 CANADIAN BANKS (Most Reliable):');
        console.log('  1. RBC Royal Bank');
        console.log('  2. TD Canada Trust');
        console.log('  3. Scotiabank');
        console.log('  4. BMO Bank of Montreal');
        console.log('  5. CIBC');
        console.log('');
        console.log('🔑 LOGIN TIPS:');
        console.log('=====================================');
        console.log('• Use your REAL bank credentials');
        console.log('• Some banks may require:');
        console.log('  - Security questions');
        console.log('  - SMS verification');
        console.log('  - Email verification');
        console.log('  - 2FA codes');
        console.log('');
        console.log('❌ AVOID THESE BANKS:');
        console.log('• Small regional banks');
        console.log('• Credit unions (some work, many don\'t)');
        console.log('• International banks');
        console.log('');
        console.log('💡 TESTING STRATEGY:');
        console.log('1. Try Chase Bank first (most reliable)');
        console.log('2. If that fails, try Bank of America');
        console.log('3. Use your actual bank login credentials');
        console.log('4. Complete any additional verification steps');
        
    } catch (error) {
        console.log('❌ Error:', error.response && error.response.data ? error.response.data : error.message);
    }
}

testSpecificBanks().catch(console.error); 