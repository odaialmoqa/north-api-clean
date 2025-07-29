// Test script to verify transaction analysis system
const axios = require('axios');

// Update this to your Railway URL
const BASE_URL = process.env.RAILWAY_URL || 'https://your-railway-app.up.railway.app';

async function testTransactionAnalysis() {
    try {
        console.log('🧪 Testing Transaction Analysis System...\n');
        
        // 1. Test health endpoint
        console.log('1. Testing health endpoint...');
        const healthResponse = await axios.get(`${BASE_URL}/health`);
        console.log('✅ Health check:', healthResponse.data.status);
        
        // 2. Test debug endpoint
        console.log('\n2. Testing debug endpoint...');
        const debugResponse = await axios.get(`${BASE_URL}/debug`);
        console.log('✅ Debug info:', {
            database: debugResponse.data.database_url_exists,
            gemini: debugResponse.data.gemini_api_key_exists,
            genai: debugResponse.data.genai_initialized
        });
        
        // 3. Register test user
        console.log('\n3. Registering test user...');
        const registerData = {
            email: 'test@example.com',
            password: 'testpassword123',
            firstName: 'Test',
            lastName: 'User'
        };
        
        let authToken;
        try {
            const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, registerData);
            authToken = registerResponse.data.token;
            console.log('✅ User registered successfully');
        } catch (error) {
            if (error.response?.status === 409) {
                // User already exists, try to login
                console.log('ℹ️ User already exists, logging in...');
                const loginResponse = await axios.post(`${BASE_URL}/api/auth/login`, {
                    email: registerData.email,
                    password: registerData.password
                });
                authToken = loginResponse.data.token;
                console.log('✅ User logged in successfully');
            } else {
                throw error;
            }
        }
        
        const headers = { Authorization: `Bearer ${authToken}` };
        
        // 4. Test insights endpoint (should be empty initially)
        console.log('\n4. Testing insights endpoint...');
        const insightsResponse = await axios.get(`${BASE_URL}/api/insights`, { headers });
        console.log('✅ Insights retrieved:', insightsResponse.data.insights.length, 'insights');
        
        // 5. Test goals endpoint
        console.log('\n5. Testing goals endpoint...');
        const goalsResponse = await axios.get(`${BASE_URL}/api/goals`, { headers });
        console.log('✅ Goals retrieved:', goalsResponse.data.length, 'goals');
        
        // 6. Test spending patterns endpoint
        console.log('\n6. Testing spending patterns endpoint...');
        const patternsResponse = await axios.get(`${BASE_URL}/api/spending-patterns`, { headers });
        console.log('✅ Spending patterns retrieved:', patternsResponse.data.patterns.length, 'patterns');
        
        // 7. Test AI chat with context
        console.log('\n7. Testing AI chat with financial context...');
        const chatResponse = await axios.post(`${BASE_URL}/api/ai/chat`, {
            message: "What are my top spending categories and how can I optimize them?"
        }, { headers });
        console.log('✅ AI chat response received:', chatResponse.data.response.substring(0, 100) + '...');
        
        // 8. Test affordability check
        console.log('\n8. Testing affordability check...');
        const affordabilityResponse = await axios.post(`${BASE_URL}/api/ai/affordability`, {
            amount: 150,
            description: "dinner out",
            category: "dining"
        }, { headers });
        console.log('✅ Affordability check:', affordabilityResponse.data.canAfford ? 'Can afford' : 'Cannot afford');
        
        console.log('\n🎉 All tests passed! Transaction analysis system is working correctly.');
        
    } catch (error) {
        console.error('❌ Test failed:', error.response?.data || error.message);
        process.exit(1);
    }
}

// Run the test
testTransactionAnalysis();