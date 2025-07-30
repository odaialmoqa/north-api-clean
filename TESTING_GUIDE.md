# North Financial - Testing Guide

## 🧪 **Complete Testing Workflow**

This guide walks you through testing the AI-powered transaction analysis system end-to-end.

## 🚀 **Backend Testing (Railway)**

### 1. **Test Railway Deployment**
```bash
node test-railway-deployment.js
```

**Expected Results:**
- ✅ Server health: OK
- ✅ Database: Connected  
- ✅ AI integration: Working (or temporarily overloaded)
- ✅ New endpoints: Deployed
- ✅ Authentication: Working

### 2. **Test Individual Endpoints**

**Health Check:**
```bash
curl https://north-api-clean-production.up.railway.app/health
```

**Create Test User:**
```bash
curl -X POST https://north-api-clean-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "testpass123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Test AI Chat:**
```bash
curl -X POST https://north-api-clean-production.up.railway.app/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"message": "What insights do you have about my spending?"}'
```

**Test Transaction Analysis:**
```bash
curl -X POST https://north-api-clean-production.up.railway.app/api/transactions/analyze \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 📱 **Mobile App Testing**

### 1. **Build the App**
```bash
cd mobile-app
./gradlew assembleDebug
```

### 2. **Test New Services**

**Test Dependency Injection:**
```kotlin
// In your test or main activity
val insightsRepository: InsightsRepository = get()
val financialAnalysisService: FinancialAnalysisService = get()

// Verify services are injected correctly
println("Services loaded: ${insightsRepository != null}")
```

**Test Data Flow:**
```kotlin
// Trigger analysis
scope.launch {
    val result = financialAnalysisService.triggerAnalysis()
    println("Analysis result: ${result.isSuccess}")
}

// Observe data updates
val insights by insightsRepository.insights.collectAsState()
LaunchedEffect(insights) {
    println("Insights updated: ${insights.size} insights")
}
```

### 3. **Test UI Integration**

**Dashboard Integration:**
- Open app → Navigate to dashboard
- Verify insights cards show placeholder data
- Check navigation to insights screen works

**Insights Screen:**
- Navigate to insights tab
- Verify empty state shows correctly
- Test refresh functionality

**Enhanced Chat:**
- Open chat screen
- Send message about spending
- Verify AI responses include context

## 🔄 **End-to-End Testing**

### **Complete User Flow Test**

1. **User Registration**
   - Register new user
   - Verify JWT token received
   - Test authenticated endpoints

2. **Plaid Connection Simulation**
   ```kotlin
   // Simulate successful Plaid connection
   fun simulatePlaidConnection() {
       scope.launch {
           // This would normally be called after real Plaid success
           financialAnalysisService.triggerAnalysis()
           
           // Wait for processing
           delay(5000)
           
           // Check results
           insightsRepository.refreshAllData()
       }
   }
   ```

3. **AI Analysis Verification**
   - Trigger transaction analysis
   - Wait for processing (30-60 seconds)
   - Verify insights generated
   - Check goals created
   - Test spending patterns

4. **UI Data Display**
   - Refresh insights screen
   - Verify data displays correctly
   - Test insight interactions
   - Check goal progress updates

## 🎯 **Specific Test Cases**

### **Test Case 1: New User Journey**
```
1. User registers → Success
2. User sees empty dashboard → Expected
3. User connects bank account → Triggers analysis
4. Analysis completes → Insights appear
5. Dashboard shows personalized data → Success
```

### **Test Case 2: AI Insight Generation**
```
1. Mock transaction data exists → Setup
2. Trigger analysis endpoint → Call API
3. Gemini processes data → AI analysis
4. Insights stored in database → Verify storage
5. Mobile app displays insights → UI update
```

### **Test Case 3: Enhanced Chat**
```
1. User asks about spending → Send message
2. System includes real data context → Backend processing
3. AI generates contextual response → Gemini response
4. User sees personalized advice → UI display
```

## 🐛 **Troubleshooting**

### **Common Issues**

**Backend Issues:**
- **503 Gemini Error**: Temporary overload, retry later
- **404 Endpoints**: Check Railway deployment status
- **Database Connection**: Verify DATABASE_URL in Railway

**Mobile App Issues:**
- **DI Errors**: Check ApiModule configuration
- **Network Errors**: Verify API base URL
- **State Updates**: Ensure StateFlow collection

**Integration Issues:**
- **No Insights**: Check Gemini API key
- **Empty Goals**: Verify transaction data exists
- **Chat Not Enhanced**: Check context enrichment

### **Debug Commands**

**Check Railway Logs:**
```bash
# If you have Railway CLI
railway logs
```

**Test API Directly:**
```bash
# Test specific endpoint
curl -v https://north-api-clean-production.up.railway.app/api/insights \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Mobile App Debugging:**
```kotlin
// Add logging to verify data flow
Log.d("TransactionAnalysis", "Insights count: ${insights.size}")
Log.d("TransactionAnalysis", "Goals count: ${goals.size}")
Log.d("TransactionAnalysis", "Loading state: $isLoading")
```

## ✅ **Success Criteria**

### **Backend Success:**
- [ ] All endpoints return 200/201
- [ ] Database queries execute successfully
- [ ] AI generates structured insights
- [ ] Goals created with priorities
- [ ] Chat responses include context

### **Mobile App Success:**
- [ ] Services inject correctly
- [ ] Data flows reactively
- [ ] UI updates automatically
- [ ] Navigation works smoothly
- [ ] Error handling graceful

### **Integration Success:**
- [ ] Plaid connection triggers analysis
- [ ] Analysis generates insights
- [ ] Insights display in UI
- [ ] Chat uses real context
- [ ] Goals track progress

## 🎉 **Expected Results**

After successful testing, you should see:

**Dashboard:**
- Dynamic insights cards with real data
- AI-generated goals with progress
- Personalized spending patterns

**Insights Screen:**
- List of AI-generated insights
- Confidence scores and action items
- Goal progress tracking
- Spending pattern analysis

**Enhanced Chat:**
- Responses reference actual spending
- Specific recommendations based on data
- Goal progress updates in conversation
- Contextual financial advice

**Example AI Response:**
> "I noticed your dining expenses increased 18% this month to $450. Based on your spending pattern, I recommend setting a $400 monthly dining budget. This could accelerate your Europe trip savings by 2 months while still allowing you to enjoy eating out 12 times per month."

## 🚀 **Next Steps After Testing**

1. **Deploy to Production**: Push final changes to Railway
2. **User Testing**: Get feedback from real users
3. **Monitor Performance**: Track insight generation rates
4. **Iterate**: Improve based on usage patterns
5. **Scale**: Optimize for more users and transactions

The testing process ensures your AI-powered transaction analysis system works end-to-end and provides real value to users!