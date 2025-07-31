const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testBackendHealth() {
    console.log('🏥 Testing Backend Health...');
    
    try {
        // Test 1: Basic health check
        console.log('\n1️⃣ Testing basic connectivity...');
        try {
            const response = await axios.get(`${BASE_URL}/`);
            console.log('✅ Backend is responding');
            console.log('Status:', response.status);
        } catch (error) {
            console.log('❌ Backend not responding:', error.message);
        }
        
        // Test 2: Plaid configuration
        console.log('\n2️⃣ Testing Plaid configuration...');
        try {
            const response = await axios.get(`${BASE_URL}/debug/plaid`);
            console.log('✅ Plaid configuration endpoint working');
            console.log('Response:', response.data);
        } catch (error) {
            console.log('❌ Plaid config endpoint failed:', error.response && error.response.data ? error.response.data : error.message);
        }
        
        // Test 3: Simple API call
        console.log('\n3️⃣ Testing simple API call...');
        try {
            const response = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
                client_name: 'Test',
                country_codes: ['US'],
                language: 'en',
                user: { client_user_id: 'test' },
                products: ['transactions']
            });
            console.log('✅ Link token creation working');
            const linkToken = response.data.link_token;
            console.log('Token:', linkToken ? linkToken.substring(0, 20) + '...' : 'No token');
        } catch (error) {
            console.log('❌ Link token creation failed:', error.response && error.response.data ? error.response.data : error.message);
        }
        
        console.log('\n🎯 BACKEND HEALTH ANALYSIS:');
        console.log('=====================================');
        console.log('The 502 errors suggest the backend is crashing');
        console.log('This could be due to:');
        console.log('1. Database connection issues');
        console.log('2. Plaid API configuration problems');
        console.log('3. Memory/CPU limits on Railway');
        console.log('4. Environment variable issues');
        console.log('');
        console.log('💡 NEXT STEPS:');
        console.log('=====================================');
        console.log('1. Check Railway logs for crash details');
        console.log('2. Verify database connection');
        console.log('3. Check Plaid credentials');
        console.log('4. Consider restarting the Railway service');
        
    } catch (error) {
        console.log('❌ Health check failed:', error.message);
    }
}

testBackendHealth().catch(console.error); 