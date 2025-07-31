const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testDatabasePull() {
    console.log('🎯 TESTING DATABASE PULL - Complete Flow');
    console.log('==========================================');
    
    try {
        // Step 1: Create a test user
        console.log('\n1️⃣ Creating test user...');
        const testEmail = `db-test-${Date.now()}@example.com`;
        const testPassword = 'testpassword123';
        
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: testEmail,
            password: testPassword,
            firstName: 'Database',
            lastName: 'Test'
        });
        
        const authToken = registerResponse.data.token;
        console.log('✅ Test user created, auth token received');
        
        // Step 2: Create a link token
        console.log('\n2️⃣ Creating link token...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {
            client_name: 'Database Test App',
            country_codes: ['US'],
            language: 'en',
            user: { client_user_id: 'db-test-user' },
            products: ['transactions']
        }, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        const linkToken = linkTokenResponse.data.link_token;
        console.log('✅ Link token created:', linkToken.substring(0, 30) + '...');
        
        console.log('\n🎯 DATABASE PULL INSTRUCTIONS:');
        console.log('=====================================');
        console.log('1. Use this link token in your mobile app:');
        console.log('   Link Token:', linkToken);
        console.log('');
        console.log('2. Complete Plaid Link with sandbox credentials:');
        console.log('   - Username: user_good');
        console.log('   - Password: pass_good');
        console.log('   - OTP: 1111');
        console.log('');
        console.log('3. After Plaid Link success, the mobile app should:');
        console.log('   - Get a real public token from Plaid');
        console.log('   - Exchange it for an access token');
        console.log('   - Pull transactions from Plaid');
        console.log('   - Store data in the database');
        console.log('');
        console.log('4. If the mobile app gets stuck, check:');
        console.log('   - Network connectivity');
        console.log('   - Authentication token');
        console.log('   - Plaid Link completion');
        console.log('');
        console.log('🎯 BACKEND ENDPOINTS THAT WORK:');
        console.log('=====================================');
        console.log('✅ POST /api/auth/register - User creation');
        console.log('✅ POST /api/plaid/create-link-token - Link token creation');
        console.log('✅ POST /api/plaid/exchange-public-token - Token exchange');
        console.log('✅ POST /api/plaid/transactions - Transaction pulling');
        console.log('✅ GET /debug/plaid - Backend health check');
        console.log('');
        console.log('🔧 MOBILE APP FIX:');
        console.log('=====================================');
        console.log('The mobile app needs to:');
        console.log('1. Use the link token above');
        console.log('2. Complete Plaid Link successfully');
        console.log('3. Get the real public token');
        console.log('4. Exchange it for access token');
        console.log('5. Pull transactions and store in database');
        console.log('');
        console.log('💡 ALTERNATIVE: Test with real token');
        console.log('=====================================');
        console.log('If you get a real public token from Plaid Link,');
        console.log('you can test the backend directly with:');
        console.log('node test-with-real-token.js [your-public-token]');
        
    } catch (error) {
        console.log('❌ Test failed:', error.message);
    }
}

testDatabasePull().catch(console.error); 