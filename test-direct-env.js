// Test to check environment variables directly
const axios = require('axios');

async function testDirectEnv() {
    try {
        console.log('🔍 Testing Direct Environment Access...\n');
        
        // Create a simple endpoint test
        const testCode = `
        app.get('/test-env-direct', (req, res) => {
            res.json({
                plaid_client_id_raw: process.env.PLAID_CLIENT_ID || 'MISSING',
                plaid_secret_exists_raw: !!process.env.PLAID_SECRET,
                plaid_env_raw: process.env.PLAID_ENV || 'MISSING',
                all_env_keys: Object.keys(process.env).filter(key => key.includes('PLAID'))
            });
        });
        `;
        
        console.log('We need to add this endpoint to the server to debug:');
        console.log(testCode);
        
        console.log('\nAlternatively, let me check if Railway logs show any startup errors...');
        
        // Try to make a request that would trigger Plaid usage
        console.log('Testing link token creation (this should work if env vars are set)...');
        
        try {
            const linkResponse = await axios.post('https://north-api-clean-production.up.railway.app/api/plaid/create-link-token');
            console.log('✅ Link token creation works - Plaid credentials are loaded!');
            console.log('Token preview:', linkResponse.data.link_token.substring(0, 20) + '...');
            
            // If link token works, the issue is elsewhere
            console.log('\n🤔 Link token works, so Plaid credentials are loaded.');
            console.log('The issue might be in the exchange logic itself.');
            
        } catch (linkError) {
            console.log('❌ Link token creation failed:', linkError.response?.data);
            console.log('This confirms Plaid credentials are not loaded properly.');
        }
        
    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

testDirectEnv();