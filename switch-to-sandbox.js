const fs = require('fs');
const path = require('path');

console.log('🔄 Switching to Plaid Sandbox Mode for Testing...');
console.log('');

console.log('📋 SANDBOX MODE BENEFITS:');
console.log('=====================================');
console.log('✅ No rate limits');
console.log('✅ No security blocks');
console.log('✅ Test credentials always work');
console.log('✅ Perfect for development/testing');
console.log('✅ Can test the full flow safely');
console.log('');

console.log('🔧 TO SWITCH TO SANDBOX:');
console.log('=====================================');
console.log('1. Update your Railway environment variables:');
console.log('   PLAID_ENV=sandbox');
console.log('   (Keep your existing PLAID_CLIENT_ID and PLAID_SECRET)');
console.log('');
console.log('2. Or temporarily modify server.js:');
console.log('   const PLAID_ENV = process.env.PLAID_ENV || \'sandbox\';');
console.log('');

console.log('🏦 SANDBOX BANKS:');
console.log('=====================================');
console.log('• Chase Bank');
console.log('• Bank of America');
console.log('• Wells Fargo');
console.log('• Capital One');
console.log('• American Express');
console.log('• Any major bank');
console.log('');

console.log('🔑 SANDBOX CREDENTIALS:');
console.log('=====================================');
console.log('Username: user_good');
console.log('Password: pass_good');
console.log('');

console.log('💡 RECOMMENDATION:');
console.log('=====================================');
console.log('1. Switch to sandbox mode temporarily');
console.log('2. Test the full flow with test credentials');
console.log('3. Once everything works, switch back to production');
console.log('4. Then test with real bank credentials');
console.log('');

console.log('🚀 QUICK FIX:');
console.log('=====================================');
console.log('Would you like me to:');
console.log('1. Update your Railway to use sandbox mode?');
console.log('2. Or modify the server.js to default to sandbox?');
console.log('3. Or wait 24 hours for rate limits to reset?'); 