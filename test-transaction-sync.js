const axios = require('axios');

const BASE_URL = 'https://north-api-clean-production.up.railway.app';

async function testTransactionSync() {
    console.log('🧪 Testing Transaction Sync Integration...\n');
    
    try {
        // Step 1: Register a test user
        console.log('1️⃣ Registering test user...');
        const registerResponse = await axios.post(`${BASE_URL}/api/auth/register`, {
            email: `test-txn-${Date.now()}@example.com`,
            password: 'testpassword123',
            firstName: 'Transaction',
            lastName: 'Tester'
        });
        
        const { token, user } = registerResponse.data;
        console.log(`✅ User registered: ${user.email}`);
        
        // Step 2: Check initial transaction status
        console.log('\n2️⃣ Checking initial transaction status...');
        const initialStatus = await axios.get(`${BASE_URL}/api/transactions/status`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        console.log('📊 Initial status:', initialStatus.data);
        
        // Step 3: Check if there are existing accounts (from previous tests)
        console.log('\n3️⃣ Checking existing accounts...');
        try {
            const accountsResponse = await axios.get(`${BASE_URL}/api/plaid/accounts`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            console.log(`✅ Found ${accountsResponse.data.accounts.length} existing accounts`);
            
            if (accountsResponse.data.accounts.length > 0) {
                // Step 4: Trigger transaction analysis
                console.log('\n4️⃣ Triggering transaction analysis...');
                const analysisResponse = await axios.post(`${BASE_URL}/api/transactions/analyze`, {}, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                console.log('✅ Analysis triggered:', analysisResponse.data);
                
                // Step 5: Wait and check transaction status
                console.log('\n5️⃣ Waiting for transaction sync...');
                await new Promise(resolve => setTimeout(resolve, 5000)); // Wait 5 seconds
                
                const finalStatus = await axios.get(`${BASE_URL}/api/transactions/status`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                console.log('📊 Final status:', finalStatus.data);
                
                // Step 6: Get actual transactions
                console.log('\n6️⃣ Getting recent transactions...');
                const transactionsResponse = await axios.get(`${BASE_URL}/api/transactions?limit=5`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                console.log('💳 Recent transactions:', JSON.stringify(transactionsResponse.data, null, 2));
                
                // Step 7: Test AI insights
                console.log('\n7️⃣ Getting AI insights...');
                const insightsResponse = await axios.get(`${BASE_URL}/api/insights`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                console.log('🧠 AI insights:', JSON.stringify(insightsResponse.data, null, 2));
            }
            
        } catch (accountsError) {
            console.log('❌ No existing accounts found');
            console.log('💡 Connect a bank account first to test transaction sync');
        }
        
    } catch (error) {
        console.error('❌ Test failed:', error.response?.data || error.message);
    }
}

testTransactionSync();