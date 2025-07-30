// Simple test to check environment variables
const axios = require('axios');

async function testEnvVars() {
    try {
        console.log('🔍 Testing Environment Variables...\n');
        
        // Make a simple request to see what the server logs
        console.log('Making request to trigger server logs...');
        
        const response = await axios.get('https://north-api-clean-production.up.railway.app/debug');
        console.log('Debug response:', JSON.stringify(response.data, null, 2));
        
        // Check if the new debug fields are there
        if (response.data.plaid_client_id_loaded) {
            console.log('\n✅ New debug fields are present - deployment worked');
        } else {
            console.log('\n❌ New debug fields missing - deployment may not have worked');
        }
        
    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

testEnvVars();