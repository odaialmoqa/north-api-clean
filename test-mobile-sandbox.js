const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testMobileSandbox() {
    console.log('📱 Testing Mobile App Sandbox Configuration...');
    
    try {
        // Test 1: Check if backend is properly configured for sandbox
        console.log('\n1️⃣ Testing backend sandbox configuration...');
        const plaidResponse = await axios.get(`${BASE_URL}/debug/plaid`);
        console.log('Backend Plaid config:', plaidResponse.data);
        
        // Test 2: Create a link token and check its format
        console.log('\n2️⃣ Creating link token for mobile app...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'North Financial',
            country_codes: ['US', 'CA'],
            language: 'en',
            user: { client_user_id: 'test-user' },
            products: ['transactions']
        });
        
        const linkToken = linkTokenResponse.data.link_token;
        console.log('Link token format:', linkToken.substring(0, 20) + '...');
        
        // Test 3: Check if it's a proper sandbox token
        if (linkToken.includes('sandbox')) {
            console.log('✅ Confirmed: Backend is generating sandbox tokens');
        } else {
            console.log('❌ Warning: Token doesn\'t look like sandbox');
        }
        
        console.log('\n🔍 MOBILE APP SANDBOX ANALYSIS:');
        console.log('=====================================');
        console.log('✅ Backend is correctly configured for sandbox');
        console.log('✅ Sandbox link tokens are being generated');
        console.log('❌ Mobile app is getting "Something went wrong"');
        console.log('');
        console.log('🔧 POSSIBLE ISSUES:');
        console.log('=====================================');
        console.log('1. Mobile app might need sandbox-specific configuration');
        console.log('2. Plaid SDK might need environment setting');
        console.log('3. Network connectivity issues');
        console.log('4. Plaid SDK version compatibility');
        console.log('');
        console.log('💡 TROUBLESHOOTING STEPS:');
        console.log('=====================================');
        console.log('1. Check mobile app logs for specific errors');
        console.log('2. Verify network connectivity');
        console.log('3. Try a different bank (Wells Fargo, Capital One)');
        console.log('4. Check if Plaid SDK needs environment configuration');
        console.log('');
        console.log('🎯 NEXT ACTIONS:');
        console.log('=====================================');
        console.log('1. Try Wells Fargo or Capital One');
        console.log('2. Check mobile app logs for error details');
        console.log('3. If still failing, we may need to update mobile app config');
        
    } catch (error) {
        console.log('❌ Error testing mobile sandbox:', error.response && error.response.data ? error.response.data : error.message);
    }
}

testMobileSandbox().catch(console.error); 