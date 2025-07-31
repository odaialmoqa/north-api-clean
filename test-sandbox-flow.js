const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testSandboxFlow() {
    console.log('🧪 Testing Sandbox Flow Details...');
    
    try {
        // Test 1: Create sandbox link token
        console.log('\n1️⃣ Creating sandbox link token...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'North Financial',
            country_codes: ['US', 'CA'],
            language: 'en',
            user: { client_user_id: 'test-user' },
            products: ['transactions']
        });
        
        const linkToken = linkTokenResponse.data.link_token;
        console.log('✅ Sandbox link token:', linkToken.substring(0, 30) + '...');
        
        console.log('\n📋 SANDBOX FLOW ANALYSIS:');
        console.log('=====================================');
        console.log('🔍 What you experienced:');
        console.log('  ✅ Plaid UI opened (good)');
        console.log('  ✅ Phone number pre-filled (normal for sandbox)');
        console.log('  ✅ Bank of America selected (good)');
        console.log('  ❌ Test credentials failed (unexpected)');
        console.log('');
        console.log('🔑 SANDBOX CREDENTIALS TO TRY:');
        console.log('=====================================');
        console.log('1. Primary test credentials:');
        console.log('   Username: user_good');
        console.log('   Password: pass_good');
        console.log('');
        console.log('2. Alternative test credentials:');
        console.log('   Username: plaid_test');
        console.log('   Password: plaid_good');
        console.log('');
        console.log('3. Bank-specific test credentials:');
        console.log('   Username: test_user');
        console.log('   Password: test_pass');
        console.log('');
        console.log('🔧 TROUBLESHOOTING STEPS:');
        console.log('=====================================');
        console.log('1. Try different test credentials above');
        console.log('2. Try a different bank (Chase, Wells Fargo)');
        console.log('3. Skip phone verification if possible');
        console.log('4. Check if there are additional verification steps');
        console.log('');
        console.log('💡 SANDBOX BEHAVIOR:');
        console.log('=====================================');
        console.log('• Phone number pre-filling is normal');
        console.log('• Some banks may still require verification');
        console.log('• Test credentials should work but may vary by bank');
        console.log('• Try "Continue as guest" if available');
        
        console.log('\n🎯 NEXT STEPS:');
        console.log('=====================================');
        console.log('1. Try the alternative test credentials');
        console.log('2. Try a different bank (Chase instead of BofA)');
        console.log('3. Look for "Continue as guest" or "Skip" options');
        console.log('4. If still failing, we may need to adjust the mobile app');
        
    } catch (error) {
        console.log('❌ Error testing sandbox flow:', error.response && error.response.data ? error.response.data : error.message);
    }
}

testSandboxFlow().catch(console.error); 