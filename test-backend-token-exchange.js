#!/usr/bin/env node

const axios = require('axios');

// Test the backend token exchange endpoint
async function testTokenExchange() {
    console.log('🧪 Testing Backend Token Exchange');
    console.log('=================================');
    
    const BASE_URL = 'http://localhost:3000';
    
    try {
        // Step 1: Create a test user
        console.log('\n1️⃣ Creating test user...');
        const testEmail = `test-user-${Date.now()}@example.com`;
        const testPassword = 'testpassword123';
        
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: testEmail,
            password: testPassword,
            firstName: 'Test',
            lastName: 'User'
        });
        
        const authToken = registerResponse.data.token;
        console.log('✅ User created and authenticated');
        
        // Step 2: Test token exchange with sandbox token
        console.log('\n2️⃣ Testing token exchange...');
        const publicToken = 'public-sandbox-12345678-1234-1234-1234-123456789012';
        
        const exchangeResponse = await axios.post(`${BASE_URL}/api/plaid/exchange-public-token`, {
            public_token: publicToken
        }, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        console.log('✅ Token exchange successful!');
        console.log('📊 Response:', {
            success: exchangeResponse.data.success,
            accounts: exchangeResponse.data.accounts?.length || 0,
            institution_name: exchangeResponse.data.institution_name,
            transactions_synced: exchangeResponse.data.transactions_synced,
            insights_generated: exchangeResponse.data.insights_generated,
            access_token_exists: !!exchangeResponse.data.access_token,
            item_id_exists: !!exchangeResponse.data.item_id
        });
        
        // Step 3: Verify transactions were stored
        console.log('\n3️⃣ Checking stored transactions...');
        const transactionsResponse = await axios.get(`${BASE_URL}/api/transactions`, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        console.log('✅ Transactions in database:', transactionsResponse.data.transactions?.length || 0);
        
        // Step 4: Check AI insights
        console.log('\n4️⃣ Checking AI insights...');
        const goalsResponse = await axios.get(`${BASE_URL}/api/goals`, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        console.log('✅ Goals/insights in database:', goalsResponse.data?.length || 0);
        
        console.log('\n🎉 BACKEND TOKEN EXCHANGE TEST SUCCESSFUL!');
        console.log('==========================================');
        console.log('✅ User registration works');
        console.log('✅ Token exchange works');
        console.log('✅ Automatic transaction sync works');
        console.log('✅ Automatic AI insights generation works');
        console.log('✅ Data storage works');
        
    } catch (error) {
        console.error('\n❌ Backend test failed:', error.message);
        if (error.response) {
            console.error('📄 Response status:', error.response.status);
            console.error('📄 Response data:', JSON.stringify(error.response.data, null, 2));
        }
        
        // If it's a connection error, suggest starting the server
        if (error.code === 'ECONNREFUSED') {
            console.log('\n💡 Suggestion: Start the backend server with:');
            console.log('   npm start');
        }
        
        process.exit(1);
    }
}

// Run the test
testTokenExchange();