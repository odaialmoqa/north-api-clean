// Quick test to check if Plaid credentials are working
const axios = require('axios');

async function quickPlaidTest() {
    try {
        console.log('🔍 Quick Plaid Test...\n');
        
        // Test link token creation - this will fail if credentials are missing
        console.log('Testing link token creation...');
        const response = await axios.post('https://north-api-clean-production.up.railway.app/api/plaid/create-link-token', {});
        
        console.log('✅ Link token created successfully!');
        console.log('🔗 Token preview:', response.data.link_token.substring(0, 30) + '...');
        console.log('\n🎉 Plaid credentials are working!');
        console.log('The mobile app should work now.');
        
    } catch (error) {
        console.log('❌ Link token creation failed');
        console.log('Status:', error.response?.status);
        console.log('Error:', error.response?.data);
        
        if (error.response?.status === 500) {
            console.log('\n💡 This suggests Plaid credentials are still not loaded.');
            console.log('Railway might need a few more minutes to restart.');
        }
    }
}

quickPlaidTest();