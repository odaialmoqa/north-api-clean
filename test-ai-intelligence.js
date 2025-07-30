// Test AI Intelligence and Response Quality
const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testAIIntelligence() {
    console.log('🧠 Testing AI Intelligence and Response Quality\n');

    try {
        // Login
        const loginResponse = await axios.post(`${BASE_URL}/api/auth/login`, {
            email: 'odaialmoqadam@gmail.com',
            password: 'test123'
        });
        const authToken = loginResponse.data.token;
        const headers = { Authorization: `Bearer ${authToken}` };

        console.log('✅ Logged in successfully\n');

        // Test 1: Check what data the AI actually has access to
        console.log('🔍 Test 1: Checking AI Data Access');
        console.log('=' .repeat(50));

        const dataCheckQuestions = [
            "What is my current account balance?",
            "How much did I spend last month?",
            "What are my top spending categories?",
            "Do I have any connected bank accounts?",
            "What transaction data do you have access to?"
        ];

        for (const question of dataCheckQuestions) {
            console.log(`\n❓ "${question}"`);
            try {
                const response = await axios.post(`${BASE_URL}/api/ai/chat`, {
                    message: question
                }, { headers });
                
                const aiResponse = response.data.response;
                console.log(`🤖 ${aiResponse.substring(0, 150)}...`);
                
                // Check if response indicates no data
                const noDataIndicators = [
                    'haven\'t connected',
                    'no accounts',
                    'don\'t have access',
                    'can\'t see',
                    'no data',
                    'connect your bank'
                ];
                
                const hasNoData = noDataIndicators.some(indicator => 
                    aiResponse.toLowerCase().includes(indicator)
                );
                
                console.log(`📊 Data Available: ${hasNoData ? '❌ No' : '✅ Yes'}`);
                
            } catch (error) {
                console.log(`❌ Error: ${error.message}`);
            }
        }

        // Test 2: Check backend data directly
        console.log('\n\n🗄️ Test 2: Checking Backend Data Directly');
        console.log('=' .repeat(50));

        try {
            const insightsResponse = await axios.get(`${BASE_URL}/api/insights`, { headers });
            console.log(`💡 Insights in database: ${insightsResponse.data.insights.length}`);
        } catch (error) {
            console.log(`❌ Insights error: ${error.response?.data?.error || error.message}`);
        }

        try {
            const goalsResponse = await axios.get(`${BASE_URL}/api/goals`, { headers });
            console.log(`🎯 Goals in database: ${goalsResponse.data.length}`);
        } catch (error) {
            console.log(`❌ Goals error: ${error.response?.data?.error || error.message}`);
        }

        try {
            const patternsResponse = await axios.get(`${BASE_URL}/api/spending-patterns`, { headers });
            console.log(`📊 Spending patterns: ${patternsResponse.data.patterns.length}`);
        } catch (error) {
            console.log(`❌ Patterns error: ${error.response?.data?.error || error.message}`);
        }

        // Test 3: Check AI prompt structure
        console.log('\n\n🎯 Test 3: Testing AI Prompt Intelligence');
        console.log('=' .repeat(50));

        const specificQuestions = [
            "I spent $500 on restaurants this month. Is that too much?",
            "My income is $5000/month and I want to save $1000. What's your advice?",
            "I have $10,000 in savings. Should I invest or keep saving?",
            "What's a good budget for someone making $60,000/year?",
            "How much should I spend on groceries vs dining out?"
        ];

        for (const question of specificQuestions) {
            console.log(`\n❓ "${question}"`);
            try {
                const response = await axios.post(`${BASE_URL}/api/ai/chat`, {
                    message: question
                }, { headers });
                
                const aiResponse = response.data.response;
                
                // Analyze response quality
                const hasGreeting = aiResponse.toLowerCase().includes('hey there') || 
                                  aiResponse.toLowerCase().includes('hi there');
                const hasNumbers = /\$[\d,]+/.test(aiResponse);
                const hasPercentages = /\d+%/.test(aiResponse);
                const hasSpecificAdvice = /recommend|suggest|should|could|try/.test(aiResponse.toLowerCase());
                const isGeneric = aiResponse.toLowerCase().includes('general') || 
                                aiResponse.toLowerCase().includes('typically') ||
                                aiResponse.toLowerCase().includes('usually');
                
                console.log(`🤖 Response: ${aiResponse.substring(0, 200)}...`);
                console.log(`📊 Analysis:`);
                console.log(`   - Has greeting: ${hasGreeting ? '❌ Yes (repetitive)' : '✅ No'}`);
                console.log(`   - Uses numbers: ${hasNumbers ? '✅ Yes' : '❌ No'}`);
                console.log(`   - Has percentages: ${hasPercentages ? '✅ Yes' : '❌ No'}`);
                console.log(`   - Specific advice: ${hasSpecificAdvice ? '✅ Yes' : '❌ No'}`);
                console.log(`   - Generic response: ${isGeneric ? '❌ Yes' : '✅ No'}`);
                
            } catch (error) {
                console.log(`❌ Error: ${error.message}`);
            }
        }

        // Test 4: Root Cause Analysis
        console.log('\n\n🔍 Root Cause Analysis');
        console.log('=' .repeat(50));
        console.log('Based on the tests above, here\'s why the AI is not intelligent:\n');
        
        console.log('1. 🗄️ NO REAL TRANSACTION DATA');
        console.log('   - Database has no actual spending transactions');
        console.log('   - AI has no patterns to analyze');
        console.log('   - Falls back to generic financial advice\n');
        
        console.log('2. 🔄 REPETITIVE GREETING SYSTEM');
        console.log('   - AI prompt includes greeting in system message');
        console.log('   - Every response starts with "Hey there!"');
        console.log('   - Needs conversation context management\n');
        
        console.log('3. 📊 MISSING DATA ENRICHMENT');
        console.log('   - AI chat doesn\'t access insights/goals/patterns');
        console.log('   - No context from user\'s financial profile');
        console.log('   - Responses lack personalization\n');
        
        console.log('4. 🎯 SOLUTION NEEDED');
        console.log('   - Connect real bank account via Plaid');
        console.log('   - Populate database with actual transactions');
        console.log('   - Run transaction analysis to generate insights');
        console.log('   - Update AI prompt to use real data context');

    } catch (error) {
        console.error('❌ Test failed:', error.response?.data || error.message);
    }
}

testAIIntelligence();