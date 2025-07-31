// Test Plaid SDK integration
const axios = require('axios');

console.log('🧪 Testing Plaid SDK Integration...');
console.log('=' .repeat(50));

async function testPlaidIntegration() {
    try {
        // Test 1: Get a link token
        console.log('\n🔗 Step 1: Getting link token...');
        const response = await axios.post('https://north-api-clean-production.up.railway.app/api/plaid/create-link-token', {
            user_id: 'test-user-' + Date.now()
        });
        
        const linkToken = response.data.link_token;
        console.log('✅ Link token received');
        console.log(`🔑 Token: ${linkToken.substring(0, 30)}...`);
        
        // Test 2: Validate token format
        console.log('\n🔍 Step 2: Validating token format...');
        if (linkToken.startsWith('link-')) {
            console.log('✅ Token starts with "link-"');
        } else {
            console.log('❌ Token does not start with "link-"');
        }
        
        if (linkToken.includes('-production-') || linkToken.includes('-sandbox-') || linkToken.includes('-development-')) {
            console.log('✅ Token contains environment');
        } else {
            console.log('❌ Token does not contain environment');
        }
        
        console.log(`📏 Token length: ${linkToken.length} characters`);
        
        // Test 3: Check if token is valid for Plaid SDK
        console.log('\n🔧 Step 3: Token analysis for Plaid SDK...');
        console.log('The token should be compatible with Plaid SDK v4.1.0');
        console.log('If the UI is not showing, possible issues:');
        console.log('1. Token format not compatible with SDK version');
        console.log('2. Network connectivity issues');
        console.log('3. Activity context not properly passed');
        console.log('4. Plaid SDK configuration issues');
        
    } catch (error) {
        console.log('❌ Test failed:', error.response && error.response.data || error.message);
    }
}

testPlaidIntegration();

console.log('\n📋 Debug Checklist:');
console.log('□ Link token is created successfully');
console.log('□ Token format is correct (link-<env>-<id>)');
console.log('□ Plaid SDK is properly initialized');
console.log('□ Activity context is available');
console.log('□ Network connectivity is working');
console.log('□ Plaid Link UI should appear');
console.log('');
console.log('🔍 If Plaid UI doesn\'t appear, check:');
console.log('- Android logs for "Plaid" or "Link" messages');
console.log('- Network connectivity on device');
console.log('- Token format compatibility');
console.log('- Activity context availability'); 