const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testBackendEndpoints() {
    console.log('🧪 Testing backend endpoints...');
    
    try {
        // Test 1: Check if server is running
        console.log('\n1️⃣ Testing server health...');
        const healthResponse = await axios.get(`${BASE_URL}/`);
        console.log('✅ Server is running');
        console.log('Response:', healthResponse.data);
        
    } catch (error) {
        console.log('❌ Server health check failed:', error.message);
    }
    
    try {
        // Test 2: Test Plaid configuration
        console.log('\n2️⃣ Testing Plaid configuration...');
        const plaidResponse = await axios.get(`${BASE_URL}/debug/plaid`);
        console.log('✅ Plaid configuration check');
        console.log('Response:', plaidResponse.data);
        
    } catch (error) {
        console.log('❌ Plaid configuration check failed:', error.message);
    }
    
    try {
        // Test 3: Test link token creation
        console.log('\n3️⃣ Testing link token creation...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'North Financial',
            country_codes: ['US', 'CA'],
            language: 'en',
            user: { client_user_id: 'test-user' },
            products: ['transactions']
        });
        console.log('✅ Link token creation successful');
        const linkToken = linkTokenResponse.data.link_token;
        console.log('Link token:', linkToken ? linkToken.substring(0, 20) + '...' : 'No token');
        
    } catch (error) {
        console.log('❌ Link token creation failed:', error.response && error.response.data ? error.response.data : error.message);
    }
    
    try {
        // Test 4: Test database connection
        console.log('\n4️⃣ Testing database connection...');
        const dbResponse = await axios.get(`${BASE_URL}/debug/db`);
        console.log('✅ Database connection successful');
        console.log('Response:', dbResponse.data);
        
    } catch (error) {
        console.log('❌ Database connection failed:', error.response && error.response.data ? error.response.data : error.message);
    }
    
    console.log('\n🎯 Backend endpoint test complete!');
}

testBackendEndpoints().catch(console.error); 