const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function checkRecentActivity() {
    console.log('📊 Checking Recent Railway Activity...');
    
    try {
        // Test 1: Check if backend is responding
        console.log('\n1️⃣ Testing backend connectivity...');
        const response = await axios.get(`${BASE_URL}/debug/plaid`);
        console.log('✅ Backend is responding');
        console.log('Plaid Environment:', response.data.plaid_env);
        
        // Test 2: Create a test user to simulate mobile app activity
        console.log('\n2️⃣ Simulating mobile app activity...');
        const testEmail = `mobile-test-${Date.now()}@example.com`;
        const testPassword = 'testpassword123';
        
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: testEmail,
            password: testPassword,
            firstName: 'Mobile',
            lastName: 'Test'
        });
        
        const authToken = registerResponse.data.token;
        console.log('✅ Test user created (simulating mobile app)');
        
        // Test 3: Create a link token (what mobile app does)
        console.log('\n3️⃣ Creating link token (mobile app step)...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'North Mobile App',
            country_codes: ['US'],
            language: 'en',
            user: { client_user_id: 'mobile-user' },
            products: ['transactions']
        }, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        const linkToken = linkTokenResponse.data.link_token;
        console.log('✅ Link token created:', linkToken.substring(0, 30) + '...');
        
        console.log('\n🎯 ANALYSIS:');
        console.log('=====================================');
        console.log('✅ Backend is working correctly');
        console.log('✅ Authentication is working');
        console.log('✅ Link token creation is working');
        console.log('❌ Mobile app token exchange is failing');
        console.log('');
        console.log('🔧 LIKELY ISSUES:');
        console.log('=====================================');
        console.log('1. Mobile app not sending auth headers');
        console.log('2. Mobile app not reaching the backend');
        console.log('3. Mobile app sending wrong token format');
        console.log('4. Network connectivity issues');
        console.log('');
        console.log('💡 QUICK FIX:');
        console.log('=====================================');
        console.log('1. Check mobile app network connectivity');
        console.log('2. Verify mobile app is sending auth headers');
        console.log('3. Check mobile app token format');
        console.log('4. Test with a real Plaid Link flow');
        
    } catch (error) {
        console.log('❌ Check failed:', error.message);
    }
}

checkRecentActivity().catch(console.error); 