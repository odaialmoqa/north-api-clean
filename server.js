const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Configuration, PlaidApi, PlaidEnvironments } = require('plaid');

// Only load .env file in development
if (process.env.NODE_ENV !== 'production') {
  require('dotenv').config();
}

// Plaid configuration
const PLAID_CLIENT_ID = process.env.PLAID_CLIENT_ID || '5fdecaa7df1def0013986738';
const PLAID_SECRET = process.env.PLAID_SECRET || '084141a287c71fd8f75cdc71c796b1';
const PLAID_ENV = process.env.PLAID_ENV || 'sandbox'; // sandbox, development, or production

console.log('=== PLAID CONFIGURATION ===');
console.log('PLAID_CLIENT_ID:', PLAID_CLIENT_ID);
console.log('PLAID_SECRET exists:', !!PLAID_SECRET);
console.log('PLAID_ENV:', PLAID_ENV);

// Initialize Plaid client
const plaidConfiguration = new Configuration({
  basePath: PlaidEnvironments[PLAID_ENV],
  baseOptions: {
    headers: {
      'PLAID-CLIENT-ID': PLAID_CLIENT_ID,
      'PLAID-SECRET': PLAID_SECRET,
    },
  },
});

const plaidClient = new PlaidApi(plaidConfiguration);

const app = express();
const port = process.env.PORT || 3000;

// Database connection with detailed logging
console.log('=== DATABASE CONNECTION DEBUG ===');
console.log('DATABASE_URL exists:', !!process.env.DATABASE_URL);
console.log('DATABASE_URL preview:', process.env.DATABASE_URL ? process.env.DATABASE_URL.substring(0, 30) + '...' : 'NOT SET');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false,
  connectionTimeoutMillis: 10000,
  idleTimeoutMillis: 30000,
  max: 10
});

// Create users table if it doesn't exist
async function initDatabase() {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        email VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        first_name VARCHAR(100),
        last_name VARCHAR(100),
        created_at TIMESTAMP DEFAULT NOW()
      );
    `);
    console.log('✅ Database tables initialized');
  } catch (error) {
    console.error('❌ Database initialization error:', error.message);
  }
}

// Test database connection on startup
async function testDatabaseConnection() {
  try {
    console.log('🔍 Testing database connection...');
    const client = await pool.connect();
    const result = await client.query('SELECT NOW()');
    console.log('✅ Database connected successfully at:', result.rows[0].now);
    client.release();
    return true;
  } catch (error) {
    console.error('❌ Database connection failed:');
    console.error('Error message:', error.message);
    console.error('Error code:', error.code);
    return false;
  }
}

// Security middleware
app.use(helmet());
app.use(cors());
app.use(express.json());

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100
});
app.use('/api/', limiter);

// Debug endpoint
app.get('/debug', (req, res) => {
  res.json({
    port: process.env.PORT || 'not set',
    node_env: process.env.NODE_ENV || 'not set',
    database_url_exists: !!process.env.DATABASE_URL,
    database_url_preview: process.env.DATABASE_URL ? process.env.DATABASE_URL.substring(0, 30) + '...' : 'NOT SET',
    jwt_secret_exists: !!process.env.JWT_SECRET
  });
});

// Health check endpoint
app.get('/health', async (req, res) => {
  try {
    console.log('Health check: Testing database connection...');
    const result = await pool.query('SELECT NOW() as current_time');
    console.log('Health check: Database query successful');
    res.json({
      status: 'OK',
      timestamp: new Date().toISOString(),
      database: 'connected',
      db_time: result.rows[0].current_time
    });
  } catch (error) {
    console.error('Health check database error:', error.message);
    console.error('Health check error code:', error.code);
    res.status(500).json({
      status: 'ERROR',
      timestamp: new Date().toISOString(),
      database: 'disconnected',
      error: error.message,
      error_code: error.code
    });
  }
});

// API info endpoint
app.get('/api', (req, res) => {
  res.json({
    name: 'North Financial API',
    version: '1.0.0-alpha',
    status: 'running'
  });
});

// Register endpoint
app.post('/api/auth/register', async (req, res) => {
  try {
    const { email, password, firstName, lastName } = req.body;

    if (!email || !password || !firstName || !lastName) {
      return res.status(400).json({ error: 'All fields are required' });
    }

    // Check if user exists
    const existingUser = await pool.query('SELECT id FROM users WHERE email = $1', [email]);
    if (existingUser.rows.length > 0) {
      return res.status(409).json({ error: 'User already exists' });
    }

    // Hash password
    const passwordHash = await bcrypt.hash(password, 12);

    // Create user
    const result = await pool.query(
      'INSERT INTO users (email, password_hash, first_name, last_name) VALUES ($1, $2, $3, $4) RETURNING id, email, first_name, last_name',
      [email, passwordHash, firstName, lastName]
    );

    const user = result.rows[0];

    // Generate JWT token
    const token = jwt.sign({ userId: user.id, email: user.email }, process.env.JWT_SECRET, { expiresIn: '24h' });

    res.status(201).json({
      message: 'Registration successful',
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name
      },
      token
    });

  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ error: 'Registration failed' });
  }
});

// Login endpoint
app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    // Get user
    const result = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const user = result.rows[0];

    // Verify password
    const isValid = await bcrypt.compare(password, user.password_hash);
    if (!isValid) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // Generate JWT token
    const token = jwt.sign({ userId: user.id, email: user.email }, process.env.JWT_SECRET, { expiresIn: '24h' });

    res.json({
      message: 'Login successful',
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name
      },
      token
    });

  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ error: 'Login failed' });
  }
});

// JWT middleware for protected routes
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }

  jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ error: 'Invalid or expired token' });
    }
    req.user = user;
    next();
  });
};

// Financial Data Endpoints

// Get user's financial summary
app.get('/api/financial/summary', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Mock financial data - replace with real data from your financial aggregation service
    const summary = {
      netWorth: 124500.50,
      totalAssets: 150000.00,
      totalLiabilities: 25500.50,
      monthlyIncome: 8200.00,
      monthlyExpenses: 5400.00,
      accounts: [
        {
          id: 'acc_1',
          name: 'RBC Checking',
          type: 'checking',
          balance: 2450.00,
          currency: 'CAD'
        },
        {
          id: 'acc_2',
          name: 'Tangerine Savings',
          type: 'savings',
          balance: 15800.00,
          currency: 'CAD'
        }
      ]
    };

    res.json(summary);
  } catch (error) {
    console.error('Financial summary error:', error);
    res.status(500).json({ error: 'Failed to fetch financial summary' });
  }
});

// Get user's goals
app.get('/api/goals', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Mock goals data - replace with real database queries
    const goals = [
      {
        id: 'goal_1',
        userId: userId,
        title: 'Emergency Fund',
        targetAmount: 10000.00,
        currentAmount: 8500.00,
        targetDate: '2025-12-31',
        priority: 'high'
      },
      {
        id: 'goal_2',
        userId: userId,
        title: 'New Car Fund',
        targetAmount: 25000.00,
        currentAmount: 12000.00,
        targetDate: '2026-06-30',
        priority: 'medium'
      }
    ];

    res.json(goals);
  } catch (error) {
    console.error('Goals fetch error:', error);
    res.status(500).json({ error: 'Failed to fetch goals' });
  }
});

// AI CFO Chat endpoint - Enhanced for Personal CFO experience with memory
app.post('/api/ai/chat', authenticateToken, async (req, res) => {
  try {
    const { message, conversationHistory = [], context, onboardingStep } = req.body;
    const userId = req.user.userId;

    // Get user info for personalization
    const userResult = await pool.query('SELECT first_name FROM users WHERE id = $1', [userId]);
    const userName = (userResult.rows[0] && userResult.rows[0].first_name) || 'there';

    // Enhanced AI CFO response with conversation memory
    let aiResponse;

    if (onboardingStep && onboardingStep !== 'COMPLETED') {
      // Handle onboarding conversation
      aiResponse = generateOnboardingResponse(message, onboardingStep, context, userName);
    } else {
      // Handle regular AI CFO conversation with memory
      aiResponse = generateRegularCFOResponse(message, conversationHistory, context, userId, userName);
    }

    res.json(aiResponse);
  } catch (error) {
    console.error('AI CFO chat error:', error);
    res.status(500).json({ error: 'AI CFO chat failed' });
  }
});

// AI CFO Affordability Check - Enhanced endpoint
app.post('/api/ai/affordability', authenticateToken, async (req, res) => {
  try {
    const { amount, description, category } = req.body;
    const userId = req.user.userId;

    // Get user info for personalization
    const userResult = await pool.query('SELECT first_name FROM users WHERE id = $1', [userId]);
    const userName = (userResult.rows[0] && userResult.rows[0].first_name) || 'there';

    // Mock financial analysis - in production, this would use real account data
    const mockBudget = {
      entertainment: { budget: 400, spent: 180, remaining: 220 },
      dining: { budget: 300, spent: 245, remaining: 55 },
      shopping: { budget: 200, spent: 95, remaining: 105 },
      emergency: { current: 8500, target: 10000 }
    };

    const canAfford = amount <= mockBudget.entertainment.remaining + mockBudget.shopping.remaining;

    const affordabilityResponse = {
      canAfford: canAfford,
      encouragingMessage: canAfford
        ? `Hey ${userName}! 🎉 Great news - you can totally afford this ${description}! You're doing such a good job managing your money.`
        : `Hi ${userName}! 💙 I want to help you make the best decision here. While ${description} sounds nice, it might stretch your budget a bit thin this month.`,
      impactOnGoals: {
        emergencyFund: canAfford
          ? "Your emergency fund progress won't be affected - you're still on track! 🎯"
          : "This might slow down your emergency fund progress by about a week, but we can adjust! 💪",
        overallImpact: canAfford ? "MINIMAL" : "MODERATE"
      },
      alternativeOptions: canAfford ? [] : [
        {
          suggestion: `Wait until next month when your ${category} budget resets`,
          reasoning: "You'll have the full budget available and won't stress about overspending!"
        },
        {
          suggestion: "Look for a similar but less expensive option",
          reasoning: "You can still enjoy what you want while staying within your comfort zone 😊"
        }
      ],
      supportiveReasoning: canAfford
        ? `Here's why I'm excited for you: You have $${mockBudget.entertainment.remaining} left in entertainment and $${mockBudget.shopping.remaining} in shopping. You're ahead on your emergency fund too! 🙌`
        : `I'm looking out for you because you're so close to your emergency fund goal! You're at $${mockBudget.emergency.current} out of $${mockBudget.emergency.target} - that's amazing progress! 🌟`,
      celebrationLevel: canAfford ? "ENTHUSIASTIC" : "GENTLE_PRAISE",
      followUpQuestions: canAfford ? [
        "Should I help you find the best deal?",
        "Want to set up a savings plan for future purchases?",
        "How are you feeling about your spending this month?"
      ] : [
        "Would you like me to suggest when you could afford this?",
        "Want to explore some alternatives?",
        "Should we look at adjusting your budget categories?"
      ]
    };

    res.json(affordabilityResponse);
  } catch (error) {
    console.error('AI affordability check error:', error);
    res.status(500).json({ error: 'Affordability check failed' });
  }
});

// AI CFO Spending Analysis
app.post('/api/ai/spending-analysis', authenticateToken, async (req, res) => {
  try {
    const { category, timeframe } = req.body;
    const userId = req.user.userId;

    // Get user info for personalization
    const userResult = await pool.query('SELECT first_name FROM users WHERE id = $1', [userId]);
    const userName = (userResult.rows[0] && userResult.rows[0].first_name) || 'there';

    // Mock spending analysis - in production, this would analyze real transaction data
    const spendingAnalysis = {
      message: `Hey ${userName}! 🕵️‍♀️ I've been analyzing your ${category} spending and I found some really interesting patterns!\n\n📊 Here's what I discovered:\n\nYou spent $127 vs your usual $85/week on groceries, but here's the cool part - that big $67 trip on the 15th included cleaning supplies and toiletries, not just food!\n\nYou're actually being super smart by stocking up on essentials. That's not overspending - that's strategic planning! 👏\n\nMystery solved! 🎉`,
      tone: 'DETECTIVE_FRIENDLY',
      insights: [
        {
          type: 'POSITIVE_TREND',
          title: 'Smart Bulk Buying',
          description: 'You saved money by buying household essentials in bulk',
          impact: 'POSITIVE',
          emoji: '🧠'
        },
        {
          type: 'SPENDING_PATTERN',
          title: 'Consistent Food Budget',
          description: 'Your actual food spending is right on track at $85/week',
          impact: 'NEUTRAL',
          emoji: '📈'
        }
      ],
      recommendations: [
        {
          title: 'Keep Up the Smart Shopping',
          description: 'Your bulk buying strategy is working great!',
          actionable: true,
          emoji: '🛒'
        }
      ],
      celebrationElements: [
        {
          type: 'EMOJI_BURST',
          content: '🎉👏🌟',
          intensity: 'MODERATE'
        }
      ]
    };

    res.json(spendingAnalysis);
  } catch (error) {
    console.error('AI spending analysis error:', error);
    res.status(500).json({ error: 'Spending analysis failed' });
  }
});

// AI CFO Onboarding initialization
app.post('/api/ai/onboarding/start', authenticateToken, async (req, res) => {
  try {
    const { userName } = req.body;
    const userId = req.user.userId;

    const welcomeResponse = {
      message: `Hey ${userName}! 👋 I'm so excited to be your personal CFO! Think of me as that supportive friend who's always in your corner, helping you make smart money decisions and cheering you on every step of the way.\n\nI'd love to get to know you better so I can create a personalized financial plan that actually fits YOUR life. No boring forms - just a friendly chat! 😊\n\nWhat brings you here today? Are you looking to save for something special, get better at budgeting, or maybe just want to feel more confident about your finances?`,
      tone: 'WARM_FRIENDLY',
      supportingData: [],
      actionableRecommendations: [],
      followUpQuestions: [
        "Tell me about your biggest financial goal right now",
        "What's your biggest money worry?",
        "I want to save for something specific",
        "Help me understand my spending better"
      ],
      celebrationElements: [
        {
          type: 'EMOJI_BURST',
          content: '🌟✨💫',
          intensity: 'MODERATE'
        }
      ],
      emojis: ['💝', '🚀', '✨'],
      encouragementLevel: 'ENCOURAGING',
      onboardingStep: 'WELCOME'
    };

    res.json(welcomeResponse);
  } catch (error) {
    console.error('AI CFO onboarding start error:', error);
    res.status(500).json({ error: 'Failed to start onboarding' });
  }
});

// Helper function to generate onboarding responses
function generateOnboardingResponse(message, step, context) {
  const lowerMessage = message.toLowerCase();

  switch (step) {
    case 'WELCOME':
      return {
        message: getWelcomeStepResponse(lowerMessage),
        tone: 'ENCOURAGING',
        followUpQuestions: [
          "I want to save for a big purchase",
          "I need help with monthly budgeting",
          "I want to build an emergency fund",
          "I'm worried about my spending habits"
        ],
        onboardingStep: 'GOALS_DISCOVERY',
        emojis: ['💡', '🎯', '✨']
      };

    case 'GOALS_DISCOVERY':
      return {
        message: getGoalsDiscoveryResponse(lowerMessage),
        tone: 'ENTHUSIASTIC',
        followUpQuestions: [
          "I love dining out and social activities",
          "I'm more of a homebody who likes simple pleasures",
          "I enjoy travel and experiences",
          "I prefer saving over spending on extras"
        ],
        onboardingStep: 'LIFESTYLE_LEARNING',
        emojis: ['🎯', '💫', '🚀']
      };

    case 'LIFESTYLE_LEARNING':
      return {
        message: "Perfect! I'm getting such a good picture of who you are and what matters to you! 😊\n\nNow, without getting too personal, could you give me a rough idea of your financial situation? I'm not looking for exact numbers - just want to understand if you're:\n\n• Just starting out and building from scratch 🌱\n• Doing okay but want to optimize and grow 📈\n• Pretty comfortable but looking for next-level strategies 🚀\n\nThis helps me give you advice that actually makes sense for where you're at right now!",
        tone: 'CARING',
        followUpQuestions: [
          "I'm just starting out with my finances",
          "I'm doing okay but want to improve",
          "I'm comfortable but want to optimize",
          "I'd rather not share specifics right now"
        ],
        onboardingStep: 'FINANCIAL_SITUATION',
        emojis: ['💭', '📊', '🎯']
      };

    default:
      return generateRegularCFOResponse(message, context);
  }
}

function getWelcomeStepResponse(lowerMessage) {
  if (lowerMessage.includes('save') || lowerMessage.includes('saving')) {
    return "That's awesome! Saving is such a smart move and I'm here to help you crush those savings goals! 💪\n\nTell me more about what you're saving for - is it something exciting like a vacation, a new car, or maybe building up that emergency fund? I love hearing about people's dreams and goals!";
  } else if (lowerMessage.includes('budget')) {
    return "Yes! Getting a handle on your budget is like giving yourself a superpower! 🦸‍♀️ Once you know where your money's going, you can make it work so much better for you.\n\nWhat's your biggest challenge with budgeting right now? Is it tracking expenses, sticking to limits, or maybe just figuring out where to start?";
  } else if (lowerMessage.includes('worry') || lowerMessage.includes('stress')) {
    return "I totally get that - money stuff can feel overwhelming sometimes, but you're taking such a positive step by wanting to tackle it! 🤗\n\nWhat's been weighing on your mind the most? Is it not having enough saved, spending too much, or maybe just feeling like you don't know if you're on the right track?";
  } else {
    return "I love that you're thinking about your financial future! That's already putting you ahead of so many people. 🌟\n\nLet's start with what matters most to you right now - what would make you feel really good about your money situation? Maybe having more saved up, spending less on certain things, or just feeling more in control?";
  }
}

function getGoalsDiscoveryResponse(lowerMessage) {
  if (lowerMessage.includes('vacation') || lowerMessage.includes('travel')) {
    return "Oh wow, a vacation - that sounds amazing! 🤩 I'm already getting excited thinking about how great it'll feel when you achieve that!\n\nNow, to help me create the perfect plan for you, tell me a bit about your lifestyle. Are you someone who loves going out with friends, prefers cozy nights in, or maybe you're all about those weekend adventures?";
  } else if (lowerMessage.includes('car') || lowerMessage.includes('house')) {
    return "That's such an exciting goal! 🚗🏠 I love helping people work toward big purchases like that - it's going to feel incredible when you reach it!\n\nTo create the best plan for you, help me understand your lifestyle a bit. Do you enjoy dining out and social activities, or are you more of a saver who prefers simple pleasures?";
  } else {
    return "That's totally okay - sometimes it takes a bit of chatting to figure out what we really want! 😊\n\nLet me ask this differently: if you could wave a magic wand and fix one thing about your money situation, what would it be? Maybe you'd have more saved up, spend less on takeout, or just feel less stressed about finances in general?";
  }
}

// Helper function to generate regular CFO responses with memory
function generateRegularCFOResponse(message, conversationHistory, context, userId, userName) {
  const lowerMessage = message.toLowerCase();
  const messageCount = conversationHistory.length;
  const previousMessages = conversationHistory.slice(-3).map(msg => msg.message && msg.message.toLowerCase() || '');

  // Create varied responses based on conversation history
  const responses = {
    greetings: [
      `Hey ${userName}! 👋 Great to see you again! What's on your financial mind today?`,
      `Hi there! 😊 I've been thinking about your financial journey - how can I help you today?`,
      `Welcome back! 🌟 Ready to tackle some financial goals together?`
    ],
    goals: [
      `I love that you're focused on your goals! 🎯 You're currently working on your emergency fund ($8,500/$10,000) and your vacation fund ($1,200/$3,000). Which one would you like to dive into?`,
      `Your goal progress is looking fantastic! 📈 Your emergency fund is at 85% - so close to that finish line! Want to talk about strategies to reach that final $1,500?`,
      `Goals are where the magic happens! ✨ I see you're making steady progress. What's motivating you most about your current goals?`
    ],
    spending: [
      `Let me put on my detective hat! 🕵️‍♀️ I've been analyzing your spending patterns and found some interesting insights...`,
      `Your spending story is actually quite positive! 📊 You're down 15% on dining out this month - that's fantastic progress!`,
      `I love diving into spending patterns with you! 💡 Here's what I'm seeing in your recent transactions...`
    ],
    affordability: [
      `Ooh, thinking about a purchase? 🛍️ I'm excited to help you figure this out! What's caught your eye?`,
      `Purchase decisions are my favorite! 💝 Tell me what you're considering and I'll help you see if it fits your budget comfortably.`,
      `Let's be smart about this together! 🧠 What are you thinking of buying? I'll check how it impacts your goals.`
    ],
    general: [
      `I'm here and ready to help with whatever's on your mind! 😊 Whether it's budgeting, goals, or just financial encouragement - what sounds good?`,
      `Thanks for chatting with me! 💬 I love being part of your financial journey. What would you like to explore today?`,
      `You always ask such thoughtful questions! 🤔 How can I support your financial success today?`
    ]
  };

  // Determine response category and avoid repetition
  let responseCategory = 'general';
  let responseArray = responses.general;

  if (lowerMessage.includes('goal')) {
    responseCategory = 'goals';
    responseArray = responses.goals;
  } else if (lowerMessage.includes('spend') || lowerMessage.includes('spending')) {
    responseCategory = 'spending';
    responseArray = responses.spending;
  } else if (lowerMessage.includes('afford') || lowerMessage.includes('buy') || lowerMessage.includes('purchase')) {
    responseCategory = 'affordability';
    responseArray = responses.affordability;
  } else if (lowerMessage.includes('hello') || lowerMessage.includes('hi') || lowerMessage.includes('hey')) {
    responseCategory = 'greetings';
    responseArray = responses.greetings;
  }

  // Select response based on message count to avoid repetition
  const responseIndex = messageCount % responseArray.length;
  const selectedMessage = responseArray[responseIndex];

  // Add contextual follow-up questions based on conversation history
  let followUpQuestions = [];

  if (responseCategory === 'goals') {
    followUpQuestions = [
      'How close am I to my emergency fund goal?',
      'Should I prioritize my vacation fund?',
      'Help me create a new savings goal',
      'What if I want to adjust my timeline?'
    ];
  } else if (responseCategory === 'spending') {
    followUpQuestions = [
      'Show me my biggest spending categories',
      'How can I optimize my grocery budget?',
      'Are there subscriptions I should cancel?',
      'Compare my spending to last month'
    ];
  } else if (responseCategory === 'affordability') {
    followUpQuestions = [
      'I want to buy something for $200',
      'Can I afford a weekend getaway?',
      'Help me plan for a big purchase',
      'What\'s my discretionary spending budget?'
    ];
  } else {
    followUpQuestions = [
      'Check my financial goals progress',
      'Analyze my recent spending patterns',
      'Help me make a purchase decision',
      'Give me some financial motivation'
    ];
  }

  return {
    message: selectedMessage,
    tone: 'WARM_FRIENDLY',
    supportingData: [
      {
        label: 'Conversation',
        value: `Message ${messageCount + 1}`,
        friendlyExplanation: "We're building a great financial conversation!",
        encouragingContext: 'I remember our previous chats and I\'m here to help! 💪',
        emoji: '💬'
      }
    ],
    followUpQuestions: followUpQuestions,
    emojis: ['😊', '💝', '🎯'],
    conversationContext: {
      messageCount: messageCount,
      category: responseCategory,
      userName: userName
    }
  };
}

// Plaid Integration Endpoints

// Create Plaid Link Token (temporarily allowing unauthenticated access for testing)
app.post('/api/plaid/create-link-token', async (req, res) => {
  try {
    const userId = 'test-user-123'; // Use test user for now

    // Create link token request - force full UI experience
    const linkTokenRequest = {
      user: {
        client_user_id: userId
      },
      client_name: 'North Financial',
      products: ['transactions'],
      country_codes: ['US', 'CA'],
      language: 'en',
      android_package_name: 'com.north.mobile',
      // Force full institution selection experience
      webhook: null, // No webhook for testing
      link_customization_name: null // Use default UI
    };

    const response = await plaidClient.linkTokenCreate(linkTokenRequest);
    const linkToken = response.data.link_token;

    res.json({
      link_token: linkToken,
      expiration: response.data.expiration
    });
  } catch (error) {
    console.error('Create link token error:', error);
    res.status(500).json({ error: 'Failed to create link token' });
  }
});

// Exchange Public Token for Access Token
app.post('/api/plaid/exchange-public-token', authenticateToken, async (req, res) => {
  try {
    const { public_token } = req.body;
    const userId = req.user.userId;

    if (!public_token) {
      return res.status(400).json({ error: 'Public token is required' });
    }

    // Exchange public token for access token
    const exchangeRequest = {
      public_token: public_token,
    };

    const exchangeResponse = await plaidClient.itemPublicTokenExchange(exchangeRequest);
    const accessToken = exchangeResponse.data.access_token;
    const itemId = exchangeResponse.data.item_id;

    // Get account information
    const accountsRequest = {
      access_token: accessToken,
    };

    const accountsResponse = await plaidClient.accountsGet(accountsRequest);
    const accounts = accountsResponse.data.accounts.map(account => ({
      id: account.account_id,
      name: account.name,
      type: account.type,
      subtype: account.subtype,
      balance: account.balances.current || 0,
      institutionName: accountsResponse.data.item.institution_id || 'Unknown Bank',
      lastSyncTime: Date.now(),
      connectionStatus: 'HEALTHY'
    }));

    // TODO: Store access token and account info in database for this user

    res.json({
      success: true,
      accounts: accounts,
      access_token: accessToken,
      item_id: itemId
    });
  } catch (error) {
    console.error('Exchange public token error:', error);
    res.status(500).json({ error: 'Failed to exchange public token' });
  }
});

// Get Connected Accounts
app.get('/api/plaid/accounts', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Mock connected accounts data
    const accounts = [
      {
        id: `acc_${userId}_1`,
        name: 'TD Checking',
        type: 'depository',
        subtype: 'checking',
        balance: 2458.32,
        institutionName: 'TD Bank',
        lastSyncTime: Date.now(),
        connectionStatus: 'HEALTHY'
      },
      {
        id: `acc_${userId}_2`,
        name: 'TD Savings',
        type: 'depository',
        subtype: 'savings',
        balance: 12042.87,
        institutionName: 'TD Bank',
        lastSyncTime: Date.now(),
        connectionStatus: 'HEALTHY'
      }
    ];

    res.json({ accounts });
  } catch (error) {
    console.error('Get accounts error:', error);
    res.status(500).json({ error: 'Failed to fetch accounts' });
  }
});

// Sync Account Transactions
app.post('/api/plaid/sync-transactions', authenticateToken, async (req, res) => {
  try {
    const { accountId } = req.body;
    const userId = req.user.userId;

    if (!accountId) {
      return res.status(400).json({ error: 'Account ID is required' });
    }

    // Mock transaction data
    const transactions = [
      {
        id: `txn_${accountId}_1`,
        accountId: accountId,
        amount: -4.50,
        description: 'Starbucks Coffee',
        category: ['Food and Drink', 'Restaurants', 'Coffee Shop'],
        date: '2024-01-15',
        merchantName: 'Starbucks'
      },
      {
        id: `txn_${accountId}_2`,
        accountId: accountId,
        amount: -85.00,
        description: 'Metro Grocery Store',
        category: ['Shops', 'Food and Beverage Store', 'Supermarkets and Groceries'],
        date: '2024-01-14',
        merchantName: 'Metro'
      },
      {
        id: `txn_${accountId}_3`,
        accountId: accountId,
        amount: -67.00,
        description: 'Gas Station',
        category: ['Transportation', 'Gas Stations'],
        date: '2024-01-13',
        merchantName: 'Shell'
      }
    ];

    res.json({
      success: true,
      transactions: transactions
    });
  } catch (error) {
    console.error('Sync transactions error:', error);
    res.status(500).json({ error: 'Failed to sync transactions' });
  }
});

// Disconnect Account
app.post('/api/plaid/disconnect-account', authenticateToken, async (req, res) => {
  try {
    const { accountId } = req.body;

    if (!accountId) {
      return res.status(400).json({ error: 'Account ID is required' });
    }

    // In production, you'd remove the account from Plaid and your database
    res.json({
      success: true,
      message: 'Account disconnected successfully'
    });
  } catch (error) {
    console.error('Disconnect account error:', error);
    res.status(500).json({ error: 'Failed to disconnect account' });
  }
});

// Get user's transactions
app.get('/api/transactions', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { limit = 50, offset = 0 } = req.query;

    // Mock transaction data - replace with real data
    const transactions = [
      {
        id: 'txn_1',
        accountId: 'acc_1',
        amount: -67.00,
        description: 'Metro Grocery Store',
        category: 'Food & Dining',
        date: '2024-11-15',
        isRecurring: false
      },
      {
        id: 'txn_2',
        accountId: 'acc_1',
        amount: -38.00,
        description: 'Loblaws',
        category: 'Food & Dining',
        date: '2024-11-17',
        isRecurring: false
      }
    ];

    res.json({
      transactions: transactions.slice(offset, offset + limit),
      total: transactions.length,
      hasMore: offset + limit < transactions.length
    });
  } catch (error) {
    console.error('Transactions fetch error:', error);
    res.status(500).json({ error: 'Failed to fetch transactions' });
  }
});

// Initialize database and test connection on startup
testDatabaseConnection();
initDatabase();

app.listen(port, () => {
  console.log(`🚀 North API running on port ${port}`);
  console.log('Environment:', process.env.NODE_ENV);
});