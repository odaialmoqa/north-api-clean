// Debug script to understand the backend exchange error
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function debugBackendExchange() {
    try {
        console.log('🔍 Debugging Backend Exchange Error...\n');
        
        // 1. Register a test user
        console.log('1️⃣ Registering test user...');
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: `debug-${Date.now()}@example.com`,
            password: 'testpassword123',
            firstName: 'Debug',
            lastName: 'User'
        });
        
        const { token } = registerResponse.data;
        console.log(`✅ User registered with token: ${token.substring(0, 20)}...`);
        
        // 2. Check backend debug info
        console.log('\n2️⃣ Checking backend configuration...');
        const debugResponse = await axios.get(`${BASE_URL}/debug`);
        console.log('🔧 Backend config:', {
            plaid_client_id: debugResponse.data.plaid_client_id ? 'SET' : 'MISSING',
            plaid_secret: debugResponse.data.plaid_secret_exists ? 'SET' : 'MISSING',
            plaid_env: debugResponse.data.plaid_env || 'NOT SET',
            database: debugResponse.data.database_url_exists ? 'CONNECTED' : 'MISSING'
        });
        
        // 3. Test with a realistic-looking public token (what Plaid actually returns)
        console.log('\n3️⃣ Testing with realistic public token format...');
        const realisticTokens = [
            'public-sandbox-b0e2c4e1-993d-4bed-b06f-5b9d2fc8722a',
            'public-development-b0e2c4e1-993d-4bed-b06f-5b9d2fc8722a',
            'public-production-b0e2c4e1-993d-4bed-b06f-5b9d2fc8722a'
        ];
        
        for (const testToken of realisticTokens) {
            console.log(`\n🧪 Testing: ${testToken.substring(0, 30)}...`);
            try {
                const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
                    public_token: testToken
                }, {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    },
                    timeout: 10000 // 10 second timeout
                });
                
                console.log('✅ Exchange successful:', exchangeResponse.data);
                break;
            } catch (exchangeError) {
                console.log('❌ Exchange failed:');
                console.log('   Status:', exchangeError.response?.status);
                console.log('   Error:', exchangeError.response?.data);
                
                // If it's a timeout or network error
                if (exchangeError.code === 'ECONNABORTED') {
                    console.log('   ⏰ Request timed out - backend might be slow');
                } else if (exchangeError.code === 'ECONNRESET') {
                    console.log('   🔌 Connection reset - backend might have crashed');
                }
            }
        }
        
        // 4. Check if the backend is still responsive
        console.log('\n4️⃣ Checking if backend is still responsive...');
        try {
            const healthResponse = await axios.get(`${BASE_URL}/health`, { timeout: 5000 });
            console.log('✅ Backend health:', healthResponse.data.status);
        } catch (healthError) {
            console.log('❌ Backend health check failed:', healthError.message);
        }
        
        // 5. Check backend logs by testing a simple endpoint
        console.log('\n5️⃣ Testing simple endpoint to verify backend is working...');
        try {
            const goalsResponse = await axios.get(`${BASE_URL}/api/goals`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            console.log('✅ Goals endpoint working:', goalsResponse.data.length, 'goals');
        } catch (goalsError) {
            console.log('❌ Goals endpoint failed:', goalsError.response?.data);
        }
        
        console.log('\n🎯 Analysis:');
        console.log('The 500 error suggests one of these issues:');
        console.log('1. 🔑 Plaid API credentials are invalid or expired');
        console.log('2. 🌐 Plaid API environment mismatch (sandbox vs production)');
        console.log('3. 📝 Public token format is unexpected');
        console.log('4. ⏰ Plaid API request is timing out');
        console.log('5. 🐛 Backend code has a bug in the exchange logic');
        
        console.log('\n💡 Next steps:');
        console.log('1. Check Railway logs for detailed error messages');
        console.log('2. Verify Plaid credentials in Railway environment variables');
        console.log('3. Ensure Plaid environment matches the public token type');
        
    } catch (error) {
        console.error('❌ Debug failed:', error.message);
    }
}

debugBackendExchange();