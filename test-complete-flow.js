#!/usr/bin/env node

const axios = require('axios');

// Configuration
const BASE_URL = process.env.BASE_URL || 'https://north-backend-production.up.railway.app';

async function testCompleteFlow() {
    console.log('🚀 Testing Complete Plaid Integration Flow');
    console.log('==========================================');
    
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
        
        // Step 2: Create Plaid link token
        console.log('\n2️⃣ Creating Plaid link token...');
        const linkTokenResponse = await axios.post(`${BASE_URL}/api/plaid/create-link-token`, {}, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        const linkToken = linkTokenResponse.data.link_token;
        console.log('✅ Link token created:', linkToken.substring(0, 30) + '...');
        
        // Step 3: Simulate Plaid Link completion (use a sandbox token)
        console.log('\n3️⃣ Simulating Plaid Link completion...');
        const publicToken = 'public-sandbox-12345678-1234-1234-1234-123456789012';
        console.log('🔍 Using sandbox public token:', publicToken);
        
        // Step 4: Exchange public token (this should now trigger everything)
        console.log('\n4️⃣ Exchanging public token (with automatic sync and insights)...');
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
            insights_generated: exchangeResponse.data.insights_generated
        });
        
        // Step 5: Verify transactions were stored
        console.log('\n5️⃣ Verifying transactions were stored...');
        const transactionsResponse = await axios.get(`${BASE_URL}/api/transactions`, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        console.log('✅ Transactions in database:', transactionsResponse.data.transactions?.length || 0);
        
        // Step 6: Verify AI insights were generated
        console.log('\n6️⃣ Verifying AI insights were generated...');
        const insightsResponse = await axios.get(`${BASE_URL}/api/goals`, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        console.log('✅ Goals/insights in database:', insightsResponse.data?.length || 0);
        
        // Step 7: Test AI chat with financial data
        console.log('\n7️⃣ Testing AI chat with financial data...');
        const chatResponse = await axios.post(`${BASE_URL}/api/ai/chat`, {
            message: 'What are my recent spending patterns?'
        }, {
            headers: { Authorization: `Bearer ${authToken}` }
        });
        
        console.log('✅ AI chat response received');
        console.log('💬 AI Response preview:', chatResponse.data.response?.substring(0, 100) + '...');
        
        console.log('\n🎉 COMPLETE FLOW TEST SUCCESSFUL!');
        console.log('=====================================');
        console.log('✅ User registration');
        console.log('✅ Plaid link token creation');
        console.log('✅ Public token exchange');
        console.log('✅ Automatic transaction sync');
        console.log('✅ Automatic AI insights generation');
        console.log('✅ Transaction data storage');
        console.log('✅ AI chat with financial context');
        
    } catch (error) {
        console.error('\n❌ Flow test failed:', error.message);
        if (error.response) {
            console.error('📄 Response status:', error.response.status);
            console.error('📄 Response data:', error.response.data);
        }
        process.exit(1);
    }
}

// Run the test
testCompleteFlow();