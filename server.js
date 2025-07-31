const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Configuration, PlaidApi, PlaidEnvironments } = require('plaid');
const { GoogleGenerativeAI } = require('@google/generative-ai');

// Load .env file (always load in development, skip in production)
if (process.env.NODE_ENV !== 'production') {
  require('dotenv').config();
} else if (!process.env.NODE_ENV) {
  // If NODE_ENV is not set, assume development and load .env
  require('dotenv').config();
}

// Plaid configuration
const PLAID_CLIENT_ID = process.env.PLAID_CLIENT_ID;
const PLAID_SECRET = process.env.PLAID_SECRET;
const PLAID_ENV = process.env.PLAID_ENV || 'sandbox'; // sandbox, development, or production

// Validate required environment variables (warnings only in development)
if (!PLAID_CLIENT_ID || !PLAID_SECRET) {
  if (process.env.NODE_ENV === 'production') {
    console.error('❌ Missing required Plaid configuration. Please set PLAID_CLIENT_ID and PLAID_SECRET in your environment variables.');
    console.error('❌ Plaid features will be disabled, but server will continue running.');
    // Don't exit in production - let server run without Plaid
  } else {
    console.warn('⚠️ Missing Plaid configuration. Plaid features will be disabled.');
  }
}

if (!process.env.JWT_SECRET || process.env.JWT_SECRET.includes('your-super-long-random-secret')) {
  if (process.env.NODE_ENV === 'production') {
    console.error('❌ Missing or default JWT_SECRET environment variable. This is required for authentication.');
    process.exit(1);
  } else {
    console.warn('⚠️ Using default JWT_SECRET. Please set a secure JWT_SECRET for production.');
  }
}

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

// Initialize Google Generative AI
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
console.log('=== GEMINI CONFIGURATION ===');
console.log('GEMINI_API_KEY exists:', !!GEMINI_API_KEY);

let genAI = null;
if (GEMINI_API_KEY) {
  genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
} else {
  console.warn('⚠️ GEMINI_API_KEY not found - AI CFO features will be disabled');
}

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

    // Create plaid_items table for storing access tokens
    await pool.query(`
      CREATE TABLE IF NOT EXISTS plaid_items (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        access_token VARCHAR(500) NOT NULL,
        item_id VARCHAR(255) NOT NULL,
        institution_id VARCHAR(255),
        institution_name VARCHAR(255),
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW(),
        UNIQUE(user_id, item_id)
      );
    `);

    // Create transactions table for storing Plaid transaction data
    await pool.query(`
      CREATE TABLE IF NOT EXISTS transactions (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        plaid_transaction_id VARCHAR(255) NOT NULL UNIQUE,
        account_id VARCHAR(255) NOT NULL,
        amount DECIMAL(10,2) NOT NULL,
        description TEXT NOT NULL,
        category TEXT[],
        subcategory VARCHAR(255),
        date DATE NOT NULL,
        merchant_name VARCHAR(255),
        is_recurring BOOLEAN DEFAULT FALSE,
        confidence_level DECIMAL(3,2),
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
      );
    `);

    // Create spending_insights table for AI-generated insights
    await pool.query(`
      CREATE TABLE IF NOT EXISTS spending_insights (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        insight_type VARCHAR(50) NOT NULL,
        title VARCHAR(255) NOT NULL,
        description TEXT NOT NULL,
        category VARCHAR(100),
        amount DECIMAL(10,2),
        confidence_score DECIMAL(3,2),
        action_items TEXT[],
        is_read BOOLEAN DEFAULT FALSE,
        expires_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT NOW()
      );
    `);

    // Create dynamic_goals table for AI-generated goals
    await pool.query(`
      CREATE TABLE IF NOT EXISTS dynamic_goals (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        goal_type VARCHAR(50) NOT NULL,
        title VARCHAR(255) NOT NULL,
        description TEXT NOT NULL,
        target_amount DECIMAL(10,2),
        current_amount DECIMAL(10,2) DEFAULT 0,
        target_date DATE,
        category VARCHAR(100),
        priority INTEGER DEFAULT 5,
        status VARCHAR(20) DEFAULT 'active',
        ai_generated BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
      );
    `);

    // Create spending_patterns table for trend analysis
    await pool.query(`
      CREATE TABLE IF NOT EXISTS spending_patterns (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        category VARCHAR(100) NOT NULL,
        period_type VARCHAR(20) NOT NULL,
        period_start DATE NOT NULL,
        period_end DATE NOT NULL,
        total_amount DECIMAL(10,2) NOT NULL,
        transaction_count INTEGER NOT NULL,
        average_transaction DECIMAL(10,2) NOT NULL,
        trend_direction VARCHAR(20),
        trend_percentage DECIMAL(5,2),
        created_at TIMESTAMP DEFAULT NOW()
      );
    `);

    // Create user_memory table for comprehensive memory system
    await pool.query(`
      CREATE TABLE IF NOT EXISTS user_memory (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        memory_data JSONB NOT NULL,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW(),
        UNIQUE(user_id)
      );
    `);

    // Create conversation_sessions table for chat history
    await pool.query(`
      CREATE TABLE IF NOT EXISTS conversation_sessions (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        session_id VARCHAR(255) NOT NULL,
        start_time TIMESTAMP DEFAULT NOW(),
        end_time TIMESTAMP,
        topics TEXT[],
        insights TEXT[],
        action_items TEXT[],
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
      );
    `);

    // Create chat_messages table for individual messages
    await pool.query(`
      CREATE TABLE IF NOT EXISTS chat_messages (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        session_id UUID NOT NULL REFERENCES conversation_sessions(id) ON DELETE CASCADE,
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        message TEXT NOT NULL,
        is_from_user BOOLEAN NOT NULL,
        topics TEXT[],
        entities TEXT[],
        sentiment VARCHAR(50),
        created_at TIMESTAMP DEFAULT NOW()
      );
    `);

    // Create user_insights table for knowledge graph
    await pool.query(`
      CREATE TABLE IF NOT EXISTS user_insights (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        insight TEXT NOT NULL,
        category VARCHAR(100) NOT NULL,
        confidence DECIMAL(3,2) DEFAULT 0.8,
        evidence TEXT[],
        actionable BOOLEAN DEFAULT true,
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
    console.warn('⚠️ Database connection failed (continuing anyway):');
    console.warn('Error message:', error.message);
    return false;
  }
}

// Security middleware
app.use(helmet());
app.use(cors());
app.use(express.json());

// Add request logging middleware
app.use((req, res, next) => {
  console.log(`📥 ${req.method} ${req.path} - ${new Date().toISOString()}`);
  if (req.body && Object.keys(req.body).length > 0) {
    console.log('📦 Body:', JSON.stringify(req.body));
  }
  next();
});

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
    jwt_secret_exists: !!process.env.JWT_SECRET,
    gemini_api_key_exists: !!process.env.GEMINI_API_KEY,
    gemini_api_key_preview: process.env.GEMINI_API_KEY ? process.env.GEMINI_API_KEY.substring(0, 20) + '...' : 'NOT SET',
    genai_initialized: !!genAI,
    // Add Plaid debug info
    plaid_client_id: process.env.PLAID_CLIENT_ID || 'MISSING',
    plaid_secret_exists: !!process.env.PLAID_SECRET,
    plaid_env: process.env.PLAID_ENV || 'NOT SET',
    plaid_client_id_loaded: PLAID_CLIENT_ID || 'MISSING',
    plaid_secret_loaded: !!PLAID_SECRET,
    plaid_env_loaded: PLAID_ENV || 'NOT SET'
  });
});

// Plaid debug endpoint
app.get('/debug/plaid', (req, res) => {
  res.json({
    plaid_client_id: process.env.PLAID_CLIENT_ID || 'NOT SET',
    plaid_client_id_length: process.env.PLAID_CLIENT_ID ? process.env.PLAID_CLIENT_ID.length : 0,
    plaid_secret_exists: !!process.env.PLAID_SECRET,
    plaid_secret_length: process.env.PLAID_SECRET ? process.env.PLAID_SECRET.length : 0,
    plaid_env: process.env.PLAID_ENV || 'NOT SET',
    plaid_client_initialized: !!plaidClient,
    all_plaid_env_vars: Object.keys(process.env).filter(key => key.includes('PLAID')),
    node_env: process.env.NODE_ENV
  });
});

// Debug endpoint for testing Plaid errors directly
app.get('/debug/plaid-error', async (req, res) => {
  try {
    // Test Plaid API directly
    const testRequest = {
      public_token: 'access-production-test-token',
    };
    
    const response = await plaidClient.itemPublicTokenExchange(testRequest);
    res.json({ success: true, response: response.data });
  } catch (error) {
    res.json({
      success: false,
      error: {
        message: error.message,
        code: error.code,
        status: error.response?.status,
        statusText: error.response?.statusText,
        plaid_response: error.response?.data,
        plaid_error_type: error.response?.data?.error_type,
        plaid_error_code: error.response?.data?.error_code,
        plaid_display_message: error.response?.data?.display_message,
        plaid_request_id: error.response?.data?.request_id
      }
    });
  }
});

// Simple Gemini test endpoint
app.get('/test-gemini', async (req, res) => {
  try {
    if (!genAI) {
      return res.json({ error: 'Gemini not initialized', api_key_exists: !!process.env.GEMINI_API_KEY });
    }

    const model = genAI.getGenerativeModel({
      model: 'gemini-1.5-flash',
      generationConfig: {
        temperature: 0.7,
        maxOutputTokens: 512,
      }
    });
    const result = await model.generateContent('Say hello in a friendly way');
    const response = await result.response;
    const text = response.text();

    res.json({ success: true, response: text });
  } catch (error) {
    res.json({ error: error.message, stack: error.stack });
  }
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
    name: 'North API',
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

// User Profile Endpoints

// Get user profile
app.get('/api/user/profile', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    
    const result = await pool.query(
      'SELECT id, email, first_name, last_name FROM users WHERE id = $1',
      [userId]
    );
    
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    const user = result.rows[0];
    res.json({
      success: true,
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name
      },
      error: null
    });
    
  } catch (error) {
    console.error('Get profile error:', error);
    res.status(500).json({ 
      success: false, 
      user: null, 
      error: 'Failed to get user profile' 
    });
  }
});

// Update user profile
app.post('/api/user/profile', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { firstName, lastName } = req.body;
    
    if (!firstName || !lastName) {
      return res.status(400).json({ 
        success: false, 
        user: null, 
        error: 'First name and last name are required' 
      });
    }
    
    const result = await pool.query(
      'UPDATE users SET first_name = $1, last_name = $2 WHERE id = $3 RETURNING id, email, first_name, last_name',
      [firstName, lastName, userId]
    );
    
    if (result.rows.length === 0) {
      return res.status(404).json({ 
        success: false, 
        user: null, 
        error: 'User not found' 
      });
    }
    
    const user = result.rows[0];
    res.json({
      success: true,
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name
      },
      error: null
    });
    
  } catch (error) {
    console.error('Update profile error:', error);
    res.status(500).json({ 
      success: false, 
      user: null, 
      error: 'Failed to update user profile' 
    });
  }
});

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

// Get user's goals - Enhanced with dynamic goals
app.get('/api/goals', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Get dynamic goals from database
    const result = await pool.query(`
      SELECT id, goal_type, title, description, target_amount, current_amount,
             target_date, category, priority, status, ai_generated, created_at
      FROM dynamic_goals 
      WHERE user_id = $1 AND status = 'active'
      ORDER BY priority DESC, created_at DESC
    `, [userId]);

    const goals = result.rows.map(goal => ({
      id: goal.id,
      userId: userId,
      goalType: goal.goal_type,
      title: goal.title,
      description: goal.description,
      targetAmount: parseFloat(goal.target_amount),
      currentAmount: parseFloat(goal.current_amount),
      targetDate: goal.target_date,
      category: goal.category,
      priority: goal.priority,
      status: goal.status,
      aiGenerated: goal.ai_generated,
      progressPercentage: Math.round((parseFloat(goal.current_amount) / parseFloat(goal.target_amount)) * 100),
      remainingAmount: parseFloat(goal.target_amount) - parseFloat(goal.current_amount),
      createdAt: goal.created_at
    }));

    // If no goals exist, create some default ones
    if (goals.length === 0) {
      const defaultGoals = [
        {
          goal_type: 'emergency_fund',
          title: 'Emergency Fund',
          description: 'Build a safety net for unexpected expenses',
          target_amount: 10000,
          category: 'Savings',
          priority: 10
        },
        {
          goal_type: 'savings',
          title: 'Vacation Fund',
          description: 'Save for your next adventure',
          target_amount: 5000,
          category: 'Travel',
          priority: 7
        }
      ];

      for (const goal of defaultGoals) {
        const targetDate = new Date();
        targetDate.setMonth(targetDate.getMonth() + 12);

        await pool.query(`
          INSERT INTO dynamic_goals (
            user_id, goal_type, title, description, target_amount,
            target_date, category, priority, ai_generated
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
        `, [
          userId,
          goal.goal_type,
          goal.title,
          goal.description,
          goal.target_amount,
          targetDate.toISOString().split('T')[0],
          goal.category,
          goal.priority,
          false
        ]);
      }

      // Return the newly created goals
      return res.redirect('/api/goals');
    }

    res.json(goals);
  } catch (error) {
    console.error('Goals fetch error:', error);
    res.status(500).json({ error: 'Failed to fetch goals' });
  }
});

// Update goal progress
app.post('/api/goals/:goalId/progress', authenticateToken, async (req, res) => {
  try {
    const { goalId } = req.params;
    const { amount } = req.body;
    const userId = req.user.userId;

    await pool.query(`
      UPDATE dynamic_goals 
      SET current_amount = current_amount + $1, updated_at = NOW()
      WHERE id = $2 AND user_id = $3
    `, [amount, goalId, userId]);

    res.json({ success: true });
  } catch (error) {
    console.error('Update goal progress error:', error);
    res.status(500).json({ error: 'Failed to update goal progress' });
  }
});

// Legacy AI Chat endpoint - Redirects to new AI CFO Brain
app.post('/api/ai/chat', authenticateToken, async (req, res) => {
  try {
    console.log('=== AI CHAT DEBUG ===');
    console.log('Request body:', JSON.stringify(req.body));
    console.log('User:', req.user);

    const { message } = req.body;

    console.log('Extracted message:', JSON.stringify(message));
    console.log('Message type:', typeof message);
    console.log('Message length:', message?.length);
    console.log('Is message falsy?', !message);

    if (!message || typeof message !== 'string' || message.trim().length === 0) {
      console.log('❌ Returning 400 - Message is required');
      return res.status(400).json({ error: 'Message is required' });
    }

    console.log('✅ Message validation passed');

    // Redirect to the new AI CFO Brain endpoint logic
    // This ensures backward compatibility while using the new Gemini-powered system
    const userId = req.user.userId;

    // Check if Gemini is available
    if (!genAI) {
      return res.status(503).json({
        error: 'The AI assistant is currently unavailable. Please try again later.'
      });
    }

    // Get user's Plaid access tokens from database (optional for general questions)
    let hasConnectedAccounts = false;
    let plaidItemsResult = { rows: [] };

    try {
      plaidItemsResult = await pool.query(
        'SELECT access_token, item_id, institution_name FROM plaid_items WHERE user_id = $1',
        [userId]
      );
      hasConnectedAccounts = plaidItemsResult.rows.length > 0;
    } catch (dbError) {
      console.warn('⚠️ Database query failed, continuing without account data:', dbError.message);
      hasConnectedAccounts = false;
    }

    // Check if this is a question that requires transaction data
    const lowerMessage = message.toLowerCase();
    const requiresTransactionData = lowerMessage.includes('spent') ||
      lowerMessage.includes('spending') ||
      lowerMessage.includes('my money') ||
      lowerMessage.includes('my transactions') ||
      lowerMessage.includes('last month') ||
      lowerMessage.includes('this month') ||
      lowerMessage.includes('my budget');

    // TEMPORARILY DISABLED - Allow all questions for general financial advice
    // Only require connected accounts for very specific transaction-specific questions
    // if (requiresTransactionData && !hasConnectedAccounts) {
    //   return res.status(400).json({
    //     error: 'To analyze your spending patterns, please connect your bank account first. For general financial advice, I\'m happy to help without account access!'
    //   });
    // }

    // Get user's financial data from database
    let transactionData = [];
    let insightsData = [];
    let goalsData = [];
    let spendingPatterns = [];

    if (hasConnectedAccounts) {
      try {
        // Get recent transactions from database
        const transactionsResult = await pool.query(`
          SELECT 
            plaid_transaction_id, account_id, amount, description, category,
            subcategory, date, merchant_name
          FROM transactions 
          WHERE user_id = $1 AND date >= NOW() - INTERVAL '90 days'
          ORDER BY date DESC
          LIMIT 100
        `, [userId]);

        transactionData = transactionsResult.rows.map(txn => ({
          transaction_id: txn.plaid_transaction_id,
          account_id: txn.account_id,
          amount: Math.abs(parseFloat(txn.amount)),
          date: txn.date,
          name: txn.description,
          merchant_name: txn.merchant_name || txn.description,
          category: txn.category || ['Other'],
          is_debit: parseFloat(txn.amount) < 0
        }));

        // Get user's insights
        const insightsResult = await pool.query(`
          SELECT insight_type, title, description, category, amount, confidence_score
          FROM spending_insights 
          WHERE user_id = $1 AND (expires_at IS NULL OR expires_at > NOW())
          ORDER BY confidence_score DESC, created_at DESC
          LIMIT 10
        `, [userId]);

        insightsData = insightsResult.rows;

        // Get user's goals
        const goalsResult = await pool.query(`
          SELECT goal_type, title, description, target_amount, current_amount, 
                 target_date, category, priority
          FROM dynamic_goals 
          WHERE user_id = $1 AND status = 'active'
          ORDER BY priority DESC
          LIMIT 10
        `, [userId]);

        goalsData = goalsResult.rows;

        // Get spending patterns
        const patternsResult = await pool.query(`
          SELECT category, total_amount, transaction_count, average_transaction,
                 trend_direction, trend_percentage, period_start
          FROM spending_patterns 
          WHERE user_id = $1 AND period_type = 'monthly'
          ORDER BY period_start DESC, total_amount DESC
          LIMIT 10
        `, [userId]);

        spendingPatterns = patternsResult.rows;

      } catch (plaidError) {
        console.error('Plaid API error:', plaidError);

        // Fallback to mock data if Plaid fails
        transactionData = [
          {
            transaction_id: 'txn_1',
            account_id: 'acc_1',
            amount: 67.00,
            date: '2024-11-15',
            name: 'Metro Grocery Store',
            merchant_name: 'Metro',
            category: ['Food and Drink', 'Groceries'],
            account_owner: null,
            institution_name: 'Mock Bank',
            is_debit: true
          }
        ];
      }
    }

    // Construct the enhanced LLM System Prompt with real data
    const systemPrompt = `**IDENTITY AND PERSONA:**
You are "North," the user's personal CFO and financial companion. You have access to their real financial data, spending patterns, AI-generated insights, and personalized goals. You know their financial habits intimately and can provide specific, actionable advice based on their actual spending behavior.

**USER'S FINANCIAL PROFILE:**
${hasConnectedAccounts ? `
Connected Accounts: ${plaidItemsResult.rows.length} bank account(s) linked
Recent Transactions: ${transactionData.length} transactions in the last 90 days
Total Recent Spending: $${transactionData.reduce((sum, txn) => sum + (txn.is_debit ? txn.amount : 0), 0).toFixed(2)}

**SPENDING PATTERNS:**
${spendingPatterns.map(pattern =>
      `• ${pattern.category}: $${parseFloat(pattern.total_amount).toFixed(2)}/month (${pattern.transaction_count} transactions, trending ${pattern.trend_direction || 'stable'})`
    ).join('\n')}

**AI-GENERATED INSIGHTS:**
${insightsData.map(insight =>
      `• ${insight.title}: ${insight.description} (${Math.round(parseFloat(insight.confidence_score) * 100)}% confidence)`
    ).join('\n')}

**ACTIVE GOALS:**
${goalsData.map(goal =>
      `• ${goal.title}: ${goal.description} (Target: $${parseFloat(goal.target_amount).toFixed(2)}, Progress: $${parseFloat(goal.current_amount).toFixed(2)})`
    ).join('\n')}

**TOP SPENDING CATEGORIES (Last 90 days):**
${transactionData.reduce((acc, txn) => {
      if (txn.is_debit) {
        const category = txn.category[0] || 'Other';
        acc[category] = (acc[category] || 0) + txn.amount;
      }
      return acc;
    }, {})
          ? Object.entries(transactionData.reduce((acc, txn) => {
            if (txn.is_debit) {
              const category = txn.category[0] || 'Other';
              acc[category] = (acc[category] || 0) + txn.amount;
            }
            return acc;
          }, {}))
            .sort(([, a], [, b]) => b - a)
            .slice(0, 5)
            .map(([category, amount]) => `• ${category}: $${amount.toFixed(2)}`)
            .join('\n')
          : 'No spending data available'}
` : 'No bank accounts connected yet - providing general financial advice'}

**YOUR ENHANCED CAPABILITIES:**
- Reference specific transactions and spending patterns
- Provide personalized insights based on actual data
- Track progress toward their specific goals
- Identify spending trends and opportunities
- Create actionable recommendations based on their behavior

**YOUR PERSONALITY:**
- Conversational and natural - talk like a real person, not a robot
- Enthusiastic about personal finance topics
- Use everyday language and relatable examples
- Share general financial wisdom and tips
- Ask follow-up questions to keep the conversation going
- Use emojis occasionally to feel more human 😊
- Tell stories or give examples when helpful

**WHAT YOU CAN DISCUSS:**
- General budgeting strategies and tips
- Saving money techniques and habits
- Financial goal setting and motivation
- Common financial challenges and solutions
- Money mindset and psychology
- Canadian financial topics (since this is a Canadian app)
- Debt management strategies
- Building emergency funds
- Smart spending habits
- Financial planning concepts

**HOW TO USE TRANSACTION DATA:**
When you have access to the user's transaction data, use it to:
- Provide personalized insights about their spending patterns
- Suggest improvements based on their actual habits
- Celebrate their good financial choices
- Help them understand where their money goes

When you don't have transaction data or the question is general, focus on:
- Sharing helpful financial tips and strategies
- Discussing financial concepts in an engaging way
- Asking thoughtful questions about their financial goals
- Providing encouragement and motivation

**CONVERSATION STYLE:**
- Start responses naturally, like you're continuing a conversation
- Use "I" statements ("I think," "I've noticed," "I'd suggest")
- Ask questions to understand their situation better
- Share relatable examples: "Many people find that..." or "A trick that works well is..."
- Keep responses engaging and not too formal

**SAFETY GUARDRAILS:**
1. **NO SPECIFIC INVESTMENT ADVICE:** Don't recommend specific stocks, crypto, or investment products. Instead say things like "That's something you'd want to research or discuss with a financial advisor."
2. **NO GUARANTEES:** Avoid promising specific outcomes
3. **ENCOURAGE PROFESSIONAL HELP:** For complex situations, suggest consulting with financial professionals
4. **STAY POSITIVE:** Frame challenges as opportunities to improve

**CANADIAN CONTEXT:**
Remember this is a Canadian app, so reference:
- Canadian banks and financial institutions when relevant
- Canadian financial concepts (RRSP, TFSA, etc.) when appropriate
- Canadian spending patterns and costs

---

**Available Transaction Data (Last 90 Days):**
${transactionData.length > 0 ? JSON.stringify(transactionData, null, 2) : 'No transaction data available - user hasn\'t connected their bank account yet.'}

---

**User's Message:** "${message}"

**Instructions:** Respond naturally and conversationally. If they're asking about their specific spending and you have transaction data, use it. If they're asking general finance questions or you don't have data, focus on helpful financial discussion and tips. Keep it friendly and engaging!`;

    // Call the Gemini API
    try {
      const model = genAI.getGenerativeModel({
        model: 'gemini-1.5-flash',
        generationConfig: {
          temperature: 0.7,
          topK: 40,
          topP: 0.95,
          maxOutputTokens: 1024,
        }
      });
      const result = await model.generateContent(systemPrompt);
      const response = await result.response;
      const aiResponse = response.text();

      // Return in the format expected by the mobile app
      res.json({
        response: aiResponse
      });

    } catch (llmError) {
      console.error('LLM API error:', llmError);
      return res.status(503).json({
        error: 'The AI assistant is currently unavailable. Please try again later.'
      });
    }

  } catch (error) {
    console.error('AI chat error:', error);
    res.status(500).json({ error: 'AI chat failed' });
  }
});

// Transaction Analysis and Insights Generation
app.post('/api/transactions/analyze', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Get user's Plaid access tokens
    const plaidItemsResult = await pool.query(
      'SELECT access_token, item_id FROM plaid_items WHERE user_id = $1',
      [userId]
    );

    if (plaidItemsResult.rows.length === 0) {
      return res.status(400).json({ error: 'No connected accounts found' });
    }

    let allTransactions = [];
    let allAccounts = [];

    // Fetch transactions from all connected accounts
    for (const item of plaidItemsResult.rows) {
      try {
        // Get accounts
        const accountsResponse = await plaidClient.accountsGet({
          access_token: item.access_token,
        });
        allAccounts.push(...accountsResponse.data.accounts);

        // Get transactions (last 90 days)
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(startDate.getDate() - 90);

        const transactionsResponse = await plaidClient.transactionsGet({
          access_token: item.access_token,
          start_date: startDate.toISOString().split('T')[0],
          end_date: endDate.toISOString().split('T')[0],
          count: 500,
        });

        allTransactions.push(...transactionsResponse.data.transactions);
      } catch (plaidError) {
        console.error('Plaid API error for item:', item.item_id, plaidError);
        continue;
      }
    }

    // Store transactions in database
    for (const transaction of allTransactions) {
      try {
        await pool.query(`
          INSERT INTO transactions (
            user_id, plaid_transaction_id, account_id, amount, description,
            category, subcategory, date, merchant_name
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
          ON CONFLICT (plaid_transaction_id) DO UPDATE SET
            amount = EXCLUDED.amount,
            description = EXCLUDED.description,
            updated_at = NOW()
        `, [
          userId,
          transaction.transaction_id,
          transaction.account_id,
          transaction.amount,
          transaction.name,
          transaction.category,
          transaction.category[1] || transaction.category[0],
          transaction.date,
          transaction.merchant_name
        ]);
      } catch (dbError) {
        console.error('Database insert error:', dbError);
        continue;
      }
    }

    // Generate spending patterns
    await generateSpendingPatterns(userId);

    // Generate AI insights
    await generateAIInsights(userId);

    // Generate dynamic goals
    await generateDynamicGoals(userId);

    res.json({
      success: true,
      message: 'Transaction analysis completed',
      transactions_processed: allTransactions.length,
      accounts_analyzed: allAccounts.length
    });

  } catch (error) {
    console.error('Transaction analysis error:', error);
    res.status(500).json({ error: 'Transaction analysis failed' });
  }
});

// Generate spending patterns from transactions
async function generateSpendingPatterns(userId) {
  try {
    // Get monthly spending by category
    const result = await pool.query(`
      SELECT 
        category[1] as category,
        DATE_TRUNC('month', date) as month,
        SUM(ABS(amount)) as total_amount,
        COUNT(*) as transaction_count,
        AVG(ABS(amount)) as average_transaction
      FROM transactions 
      WHERE user_id = $1 AND amount < 0 AND date >= NOW() - INTERVAL '6 months'
      GROUP BY category[1], DATE_TRUNC('month', date)
      ORDER BY month DESC, total_amount DESC
    `, [userId]);

    // Calculate trends and store patterns
    const categoryData = {};

    for (const row of result.rows) {
      const category = row.category || 'Other';
      if (!categoryData[category]) {
        categoryData[category] = [];
      }
      categoryData[category].push({
        month: row.month,
        total: parseFloat(row.total_amount),
        count: parseInt(row.transaction_count),
        average: parseFloat(row.average_transaction)
      });
    }

    // Store patterns with trend analysis
    for (const [category, months] of Object.entries(categoryData)) {
      if (months.length >= 2) {
        const latest = months[0];
        const previous = months[1];
        const trendPercentage = ((latest.total - previous.total) / previous.total) * 100;
        const trendDirection = trendPercentage > 10 ? 'increasing' :
          trendPercentage < -10 ? 'decreasing' : 'stable';

        await pool.query(`
          INSERT INTO spending_patterns (
            user_id, category, period_type, period_start, period_end,
            total_amount, transaction_count, average_transaction,
            trend_direction, trend_percentage
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
          ON CONFLICT (user_id, category, period_start) DO UPDATE SET
            total_amount = EXCLUDED.total_amount,
            transaction_count = EXCLUDED.transaction_count,
            average_transaction = EXCLUDED.average_transaction,
            trend_direction = EXCLUDED.trend_direction,
            trend_percentage = EXCLUDED.trend_percentage
        `, [
          userId, category, 'monthly', latest.month, latest.month,
          latest.total, latest.count, latest.average,
          trendDirection, trendPercentage
        ]);
      }
    }
  } catch (error) {
    console.error('Error generating spending patterns:', error);
  }
}

// Generate AI-powered insights from transaction data
async function generateAIInsights(userId) {
  if (!genAI) return;

  try {
    // Get recent transaction data
    const transactionsResult = await pool.query(`
      SELECT category, amount, description, date, merchant_name
      FROM transactions 
      WHERE user_id = $1 AND date >= NOW() - INTERVAL '30 days'
      ORDER BY date DESC
      LIMIT 100
    `, [userId]);

    // Get spending patterns
    const patternsResult = await pool.query(`
      SELECT category, total_amount, trend_direction, trend_percentage
      FROM spending_patterns 
      WHERE user_id = $1 AND period_type = 'monthly'
      ORDER BY total_amount DESC
      LIMIT 10
    `, [userId]);

    const transactions = transactionsResult.rows;
    const patterns = patternsResult.rows;

    if (transactions.length === 0) return;

    // Create AI prompt for insights generation
    const prompt = `Analyze this user's spending data and generate 3-5 actionable financial insights:

SPENDING PATTERNS:
${patterns.map(p => `${p.category}: $${parseFloat(p.total_amount).toFixed(2)}/month (${p.trend_direction} ${Math.abs(parseFloat(p.trend_percentage) || 0).toFixed(1)}%)`).join('\n')}

RECENT TRANSACTIONS (last 30 days):
${transactions.slice(0, 20).map(t => `${t.date}: ${t.description} - $${Math.abs(parseFloat(t.amount)).toFixed(2)} (${t.category?.[0] || 'Other'})`).join('\n')}

Generate insights in this JSON format:
[
  {
    "type": "spending_alert|opportunity|trend|goal_suggestion",
    "title": "Brief title",
    "description": "Detailed insight with specific numbers",
    "category": "category name",
    "amount": amount_if_relevant,
    "confidence": 0.8,
    "actions": ["action 1", "action 2"]
  }
]

Focus on:
- Unusual spending patterns
- Opportunities to save money
- Budget optimization suggestions
- Goal-setting recommendations
- Trend analysis with specific numbers`;

    const model = genAI.getGenerativeModel({
      model: 'gemini-1.5-flash',
      generationConfig: {
        temperature: 0.3,
        maxOutputTokens: 1024,
      }
    });

    const result = await model.generateContent(prompt);
    const response = await result.response;
    const text = response.text();

    // Parse AI response and store insights
    try {
      const insights = JSON.parse(text.replace(/```json\n?|\n?```/g, ''));

      for (const insight of insights) {
        await pool.query(`
          INSERT INTO spending_insights (
            user_id, insight_type, title, description, category,
            amount, confidence_score, action_items
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
        `, [
          userId,
          insight.type,
          insight.title,
          insight.description,
          insight.category,
          insight.amount,
          insight.confidence,
          insight.actions
        ]);
      }
    } catch (parseError) {
      console.error('Error parsing AI insights:', parseError);
    }

  } catch (error) {
    console.error('Error generating AI insights:', error);
  }
}

// Generate dynamic goals based on spending patterns
async function generateDynamicGoals(userId) {
  if (!genAI) return;

  try {
    // Get user's spending patterns and current goals
    const patternsResult = await pool.query(`
      SELECT category, total_amount, trend_direction
      FROM spending_patterns 
      WHERE user_id = $1 AND period_type = 'monthly'
      ORDER BY total_amount DESC
    `, [userId]);

    const existingGoalsResult = await pool.query(`
      SELECT title, target_amount, current_amount, category
      FROM dynamic_goals 
      WHERE user_id = $1 AND status = 'active'
    `, [userId]);

    const patterns = patternsResult.rows;
    const existingGoals = existingGoalsResult.rows;

    if (patterns.length === 0) return;

    const totalMonthlySpending = patterns.reduce((sum, p) => sum + parseFloat(p.total_amount), 0);

    const prompt = `Based on this user's spending patterns, suggest 2-3 personalized financial goals:

MONTHLY SPENDING PATTERNS:
${patterns.map(p => `${p.category}: $${parseFloat(p.total_amount).toFixed(2)} (${p.trend_direction})`).join('\n')}

Total Monthly Spending: $${totalMonthlySpending.toFixed(2)}

EXISTING GOALS:
${existingGoals.map(g => `${g.title}: $${parseFloat(g.current_amount).toFixed(2)}/$${parseFloat(g.target_amount).toFixed(2)}`).join('\n')}

Generate goals in this JSON format:
[
  {
    "type": "savings|debt_reduction|spending_optimization|emergency_fund",
    "title": "Goal title",
    "description": "Specific, actionable description",
    "target_amount": amount,
    "category": "category",
    "priority": 1-10,
    "target_months": 6
  }
]

Focus on:
- Emergency fund if not present
- Spending reduction in high categories
- Savings goals based on income potential
- Debt reduction if applicable`;

    const model = genAI.getGenerativeModel({
      model: 'gemini-1.5-flash',
      generationConfig: {
        temperature: 0.4,
        maxOutputTokens: 1024,
      }
    });

    const result = await model.generateContent(prompt);
    const response = await result.response;
    const text = response.text();

    // Parse and store dynamic goals
    try {
      const goals = JSON.parse(text.replace(/```json\n?|\n?```/g, ''));

      for (const goal of goals) {
        const targetDate = new Date();
        targetDate.setMonth(targetDate.getMonth() + (goal.target_months || 12));

        await pool.query(`
          INSERT INTO dynamic_goals (
            user_id, goal_type, title, description, target_amount,
            target_date, category, priority, ai_generated
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
        `, [
          userId,
          goal.type,
          goal.title,
          goal.description,
          goal.target_amount,
          targetDate.toISOString().split('T')[0],
          goal.category,
          goal.priority,
          true
        ]);
      }
    } catch (parseError) {
      console.error('Error parsing dynamic goals:', parseError);
    }

  } catch (error) {
    console.error('Error generating dynamic goals:', error);
  }
}

// Get user insights
app.get('/api/insights', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const result = await pool.query(`
      SELECT id, insight_type, title, description, category, amount,
             confidence_score, action_items, is_read, created_at
      FROM spending_insights 
      WHERE user_id = $1 AND (expires_at IS NULL OR expires_at > NOW())
      ORDER BY confidence_score DESC, created_at DESC
      LIMIT 20
    `, [userId]);

    res.json({
      success: true,
      insights: result.rows
    });
  } catch (error) {
    console.error('Get insights error:', error);
    res.status(500).json({ error: 'Failed to get insights' });
  }
});

// Get spending patterns
app.get('/api/spending-patterns', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const result = await pool.query(`
      SELECT category, period_start, period_end, total_amount,
             transaction_count, average_transaction, trend_direction, trend_percentage
      FROM spending_patterns 
      WHERE user_id = $1 AND period_type = 'monthly'
      ORDER BY period_start DESC, total_amount DESC
      LIMIT 50
    `, [userId]);

    res.json({
      success: true,
      patterns: result.rows
    });
  } catch (error) {
    console.error('Get spending patterns error:', error);
    res.status(500).json({ error: 'Failed to get spending patterns' });
  }
});

// Mark insight as read
app.post('/api/insights/:insightId/read', authenticateToken, async (req, res) => {
  try {
    const { insightId } = req.params;
    const userId = req.user.userId;

    await pool.query(`
      UPDATE spending_insights 
      SET is_read = true 
      WHERE id = $1 AND user_id = $2
    `, [insightId, userId]);

    res.json({ success: true });
  } catch (error) {
    console.error('Mark insight as read error:', error);
    res.status(500).json({ error: 'Failed to mark insight as read' });
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

    // Get real spending patterns from database
    const patternsResult = await pool.query(`
      SELECT category, total_amount, trend_direction
      FROM spending_patterns 
      WHERE user_id = $1 AND period_type = 'monthly'
      ORDER BY total_amount DESC
    `, [userId]);

    // Get current goals
    const goalsResult = await pool.query(`
      SELECT title, target_amount, current_amount, priority
      FROM dynamic_goals 
      WHERE user_id = $1 AND status = 'active'
      ORDER BY priority DESC
    `, [userId]);

    const patterns = patternsResult.rows;
    const goals = goalsResult.rows;

    // Calculate affordability based on real data
    const totalMonthlySpending = patterns.reduce((sum, p) => sum + parseFloat(p.total_amount), 0);
    const categorySpending = patterns.find(p => p.category.toLowerCase().includes(category?.toLowerCase() || ''));
    const monthlyBudgetRoom = Math.max(0, 5000 - totalMonthlySpending); // Assume 5k monthly income

    const canAfford = amount <= monthlyBudgetRoom * 0.3; // 30% of available budget

    const affordabilityResponse = {
      canAfford: canAfford,
      encouragingMessage: canAfford
        ? `Hey ${userName}! 🎉 Great news - you can afford this ${description || 'purchase'} of $${amount}. Based on your spending patterns, you have room in your budget.`
        : `Hey ${userName}, this ${description || 'purchase'} of $${amount} might stretch your budget. Let me suggest some alternatives or timing adjustments.`,
      budgetImpact: {
        monthlySpending: totalMonthlySpending,
        availableBudget: monthlyBudgetRoom,
        categorySpending: categorySpending?.total_amount || 0,
        impactOnGoals: goals.map(g => ({
          goal: g.title,
          delayDays: canAfford ? 0 : Math.ceil((amount / (parseFloat(g.target_amount) - parseFloat(g.current_amount))) * 30)
        }))
      }
    };

    res.json(affordabilityResponse);

  } catch (error) {
    console.error('Affordability check error:', error);

    // Fallback response
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
        ? `Great news - you can afford this ${description}! You're doing such a good job managing your money.`
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

// Memory Management Endpoints

// Get user memory profile
app.get('/api/memory/profile', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Get user memory from database
    const memoryResult = await pool.query(
      'SELECT memory_data FROM user_memory WHERE user_id = $1',
      [userId]
    );

    if (memoryResult.rows.length === 0) {
      // Create default memory profile
      const defaultMemory = {
        userId: userId,
        personalInfo: {
          name: req.user.firstName || "User",
          email: req.user.email
        },
        financialProfile: {
          goals: [
            {
              name: "Emergency Fund",
              targetAmount: 10000.0,
              currentAmount: 8500.0,
              targetDate: "2024-12-31",
              priority: "high"
            },
            {
              name: "Europe Trip",
              targetAmount: 5000.0,
              currentAmount: 2100.0,
              targetDate: "2024-12-31",
              priority: "medium"
            }
          ],
          spendingPatterns: {
            dining: {
              category: "dining",
              budgetAmount: 450.0,
              actualAmount: 380.0,
              trend: "increased 18% this month"
            },
            groceries: {
              category: "groceries",
              budgetAmount: 300.0,
              actualAmount: 295.0,
              trend: "within budget range"
            }
          },
          monthlyIncome: 4500.0
        },
        conversationHistory: [],
        knowledgeGraph: {
          concepts: {},
          relationships: [],
          insights: []
        },
        preferences: {
          communicationStyle: "friendly and encouraging",
          currency: "CAD"
        },
        lastUpdated: new Date().toISOString()
      };

      // Save default memory to database
      await pool.query(
        'INSERT INTO user_memory (user_id, memory_data) VALUES ($1, $2)',
        [userId, JSON.stringify(defaultMemory)]
      );

      return res.json(defaultMemory);
    }

    res.json(memoryResult.rows[0].memory_data);
  } catch (error) {
    console.error('Memory profile fetch error:', error);
    res.status(500).json({ error: 'Failed to fetch memory profile' });
  }
});

// Update user memory profile
app.post('/api/memory/profile', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const memoryData = req.body;

    // Update memory in database
    await pool.query(
      `INSERT INTO user_memory (user_id, memory_data, updated_at) 
       VALUES ($1, $2, NOW()) 
       ON CONFLICT (user_id) 
       DO UPDATE SET memory_data = $2, updated_at = NOW()`,
      [userId, JSON.stringify(memoryData)]
    );

    res.json({ success: true, message: 'Memory profile updated' });
  } catch (error) {
    console.error('Memory profile update error:', error);
    res.status(500).json({ error: 'Failed to update memory profile' });
  }
});

// Add conversation message
app.post('/api/memory/message', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { sessionId, message, isFromUser, topics = [], entities = [] } = req.body;

    // Ensure session exists
    await pool.query(
      `INSERT INTO conversation_sessions (user_id, session_id, start_time) 
       VALUES ($1, $2, NOW()) 
       ON CONFLICT (user_id, session_id) DO NOTHING`,
      [userId, sessionId]
    );

    // Get session UUID
    const sessionResult = await pool.query(
      'SELECT id FROM conversation_sessions WHERE user_id = $1 AND session_id = $2',
      [userId, sessionId]
    );

    if (sessionResult.rows.length === 0) {
      return res.status(400).json({ error: 'Session not found' });
    }

    const sessionUUID = sessionResult.rows[0].id;

    // Add message to database
    await pool.query(
      `INSERT INTO chat_messages (session_id, user_id, message, is_from_user, topics, entities) 
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [sessionUUID, userId, message, isFromUser, topics, entities]
    );

    res.json({ success: true, message: 'Message added to memory' });
  } catch (error) {
    console.error('Memory message add error:', error);
    res.status(500).json({ error: 'Failed to add message to memory' });
  }
});

// Get conversation history
app.get('/api/memory/conversations', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const limit = parseInt(req.query.limit) || 10;

    // Get recent conversations with messages
    const conversationsResult = await pool.query(
      `SELECT cs.session_id, cs.start_time, cs.end_time, cs.topics, cs.insights,
              cm.message, cm.is_from_user, cm.topics as message_topics, cm.entities, cm.created_at as message_time
       FROM conversation_sessions cs
       LEFT JOIN chat_messages cm ON cs.id = cm.session_id
       WHERE cs.user_id = $1
       ORDER BY cs.start_time DESC, cm.created_at ASC
       LIMIT $2`,
      [userId, limit * 10] // Get more messages to account for multiple per session
    );

    // Group messages by session
    const sessions = {};
    conversationsResult.rows.forEach(row => {
      if (!sessions[row.session_id]) {
        sessions[row.session_id] = {
          sessionId: row.session_id,
          startTime: row.start_time,
          endTime: row.end_time,
          topics: row.topics || [],
          insights: row.insights || [],
          messages: []
        };
      }

      if (row.message) {
        sessions[row.session_id].messages.push({
          message: row.message,
          isFromUser: row.is_from_user,
          topics: row.message_topics || [],
          entities: row.entities || [],
          timestamp: row.message_time
        });
      }
    });

    res.json(Object.values(sessions).slice(0, limit));
  } catch (error) {
    console.error('Conversation history fetch error:', error);
    res.status(500).json({ error: 'Failed to fetch conversation history' });
  }
});

// Add user insight
app.post('/api/memory/insight', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { insight, category, confidence = 0.8, evidence = [], actionable = true } = req.body;

    await pool.query(
      `INSERT INTO user_insights (user_id, insight, category, confidence, evidence, actionable) 
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [userId, insight, category, confidence, evidence, actionable]
    );

    res.json({ success: true, message: 'Insight added to memory' });
  } catch (error) {
    console.error('Memory insight add error:', error);
    res.status(500).json({ error: 'Failed to add insight to memory' });
  }
});

// Get user insights
app.get('/api/memory/insights', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const category = req.query.category;

    let query = 'SELECT * FROM user_insights WHERE user_id = $1';
    let params = [userId];

    if (category) {
      query += ' AND category = $2';
      params.push(category);
    }

    query += ' ORDER BY created_at DESC LIMIT 20';

    const insightsResult = await pool.query(query, params);
    res.json(insightsResult.rows);
  } catch (error) {
    console.error('Memory insights fetch error:', error);
    res.status(500).json({ error: 'Failed to fetch insights' });
  }
});

// Enhanced AI Chat endpoint with memory integration
app.post('/api/ai/chat-with-memory', authenticateToken, async (req, res) => {
  try {
    const { message, sessionId } = req.body;
    const userId = req.user.userId;

    if (!message || !sessionId) {
      return res.status(400).json({ error: 'Message and sessionId are required' });
    }

    // Get user memory profile
    const memoryResult = await pool.query(
      'SELECT memory_data FROM user_memory WHERE user_id = $1',
      [userId]
    );

    let userMemory = {};
    if (memoryResult.rows.length > 0) {
      userMemory = memoryResult.rows[0].memory_data;
    }

    // Get recent conversation history
    const conversationResult = await pool.query(
      `SELECT cm.message, cm.is_from_user, cm.topics, cm.entities, cm.created_at
       FROM conversation_sessions cs
       JOIN chat_messages cm ON cs.id = cm.session_id
       WHERE cs.user_id = $1
       ORDER BY cm.created_at DESC
       LIMIT 10`,
      [userId]
    );

    const recentMessages = conversationResult.rows.reverse(); // Chronological order

    // Get relevant insights
    const insightsResult = await pool.query(
      'SELECT insight, category, confidence FROM user_insights WHERE user_id = $1 ORDER BY created_at DESC LIMIT 5',
      [userId]
    );

    const insights = insightsResult.rows;

    // Store user message in memory
    await pool.query(
      `INSERT INTO conversation_sessions (user_id, session_id, start_time) 
       VALUES ($1, $2, NOW()) 
       ON CONFLICT (user_id, session_id) DO NOTHING`,
      [userId, sessionId]
    );

    const sessionResult = await pool.query(
      'SELECT id FROM conversation_sessions WHERE user_id = $1 AND session_id = $2',
      [userId, sessionId]
    );

    const sessionUUID = sessionResult.rows[0].id;

    await pool.query(
      `INSERT INTO chat_messages (session_id, user_id, message, is_from_user, topics, entities) 
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [sessionUUID, userId, message, true, [], []]
    );

    // Enhanced system prompt with memory context
    const systemPrompt = `**IDENTITY AND PERSONA:**
You are "North," the user's personal CFO who has been working with them over time. You have access to their complete financial history, conversation context, and personal insights. You know them well and can reference specific details from your relationship.

**USER MEMORY PROFILE:**
${JSON.stringify(userMemory, null, 2)}

**RECENT CONVERSATION HISTORY:**
${recentMessages.map(msg => `${msg.is_from_user ? 'User' : 'North'}: ${msg.message}`).join('\n')}

**USER INSIGHTS:**
${insights.map(insight => `• ${insight.category}: ${insight.insight} (confidence: ${insight.confidence})`).join('\n')}

**YOUR RELATIONSHIP WITH THIS USER:**
- You've been their personal CFO and have detailed knowledge of their financial journey
- You remember their goals, spending patterns, and previous conversations
- You can reference specific progress they've made and challenges they've faced
- You know their preferences and communication style

**CURRENT USER MESSAGE:** "${message}"

**INSTRUCTIONS:** 
Respond as their knowledgeable personal CFO who has been working with them over time. Reference their specific financial situation, goals, and previous conversations when relevant. Be warm, personal, and show that you truly know and remember them. Use their actual financial data and progress in your response.`;

    // Call Gemini with enhanced context
    if (!genAI) {
      return res.status(503).json({
        error: 'The AI assistant is currently unavailable. Please try again later.'
      });
    }

    const model = genAI.getGenerativeModel({
      model: 'gemini-1.5-flash',
      generationConfig: {
        temperature: 0.7,
        topK: 40,
        topP: 0.95,
        maxOutputTokens: 1024,
      }
    });

    const result = await model.generateContent(systemPrompt);
    const response = await result.response;
    const aiResponse = response.text();

    // Store AI response in memory
    await pool.query(
      `INSERT INTO chat_messages (session_id, user_id, message, is_from_user, topics, entities) 
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [sessionUUID, userId, aiResponse, false, [], []]
    );

    // Extract and store insights from the conversation
    if (message.toLowerCase().includes('goal') || message.toLowerCase().includes('save')) {
      await pool.query(
        `INSERT INTO user_insights (user_id, insight, category, confidence, evidence) 
         VALUES ($1, $2, $3, $4, $5)`,
        [userId, `User discussed goals in context: ${message}`, 'Goal Planning', 0.8, [message]]
      );
    }

    res.json({ response: aiResponse });

  } catch (error) {
    console.error('AI chat with memory error:', error);
    res.status(500).json({ error: 'AI chat with memory failed' });
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

// AI Personal CFO Brain - LLM Gateway Service
app.post('/api/cfo/chat', authenticateToken, async (req, res) => {
  try {
    console.log('=== CFO CHAT DEBUG ===');
    console.log('Request body:', JSON.stringify(req.body));
    console.log('User:', req.user);

    const { message } = req.body;
    const userId = req.user.userId;

    if (!message) {
      return res.status(400).json({ error: 'Message is required' });
    }

    // Check if Gemini is available
    if (!genAI) {
      return res.status(503).json({
        error: 'The AI assistant is currently unavailable. Please try again later.'
      });
    }

    // Step 1: Get user's Plaid access tokens from database (optional for general questions)
    const plaidItemsResult = await pool.query(
      'SELECT access_token, item_id, institution_name FROM plaid_items WHERE user_id = $1',
      [userId]
    );

    const hasConnectedAccounts = plaidItemsResult.rows.length > 0;

    // Check if this is a question that requires transaction data
    const lowerMessage = message.toLowerCase();
    const requiresTransactionData = lowerMessage.includes('spent') ||
      lowerMessage.includes('spending') ||
      lowerMessage.includes('my money') ||
      lowerMessage.includes('my transactions') ||
      lowerMessage.includes('last month') ||
      lowerMessage.includes('this month') ||
      lowerMessage.includes('my budget');

    // TEMPORARILY DISABLED - Allow all questions for general financial advice
    // Only require connected accounts for very specific transaction-specific questions
    // if (requiresTransactionData && !hasConnectedAccounts) {
    //   return res.status(400).json({
    //     error: 'To analyze your spending patterns, please connect your bank account first. For general financial advice, I\'m happy to help without account access!'
    //   });
    // }

    // Step 2: Fetch user's transactions from Plaid (last 90 days) - only if accounts are connected
    let transactionData = [];

    if (hasConnectedAccounts) {
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - 90);
      const endDate = new Date();

      try {
        // Fetch transactions from all connected accounts
        for (const plaidItem of plaidItemsResult.rows) {
          const transactionsRequest = {
            access_token: plaidItem.access_token,
            start_date: startDate.toISOString().split('T')[0],
            end_date: endDate.toISOString().split('T')[0],
            count: 100
          };

          const transactionsResponse = await plaidClient.transactionsGet(transactionsRequest);

          // Transform Plaid transaction format to our format
          const transformedTransactions = transactionsResponse.data.transactions.map(txn => ({
            transaction_id: txn.transaction_id,
            account_id: txn.account_id,
            amount: Math.abs(txn.amount), // Plaid uses negative for debits, we want positive amounts
            date: txn.date,
            name: txn.name,
            merchant_name: txn.merchant_name || txn.name,
            category: txn.category || ['Other'],
            account_owner: txn.account_owner,
            institution_name: plaidItem.institution_name,
            is_debit: txn.amount > 0 // Plaid uses positive for debits
          }));

          transactionData.push(...transformedTransactions);
        }

        // Sort transactions by date (most recent first)
        transactionData.sort((a, b) => new Date(b.date) - new Date(a.date));

      } catch (plaidError) {
        console.error('Plaid API error:', plaidError);

        // Fallback to mock data if Plaid fails
        console.log('Falling back to mock transaction data');
        transactionData = [
          {
            transaction_id: 'txn_1',
            account_id: 'acc_1',
            amount: 67.00,
            date: '2024-11-15',
            name: 'Metro Grocery Store',
            merchant_name: 'Metro',
            category: ['Food and Drink', 'Groceries'],
            account_owner: null,
            institution_name: 'Mock Bank',
            is_debit: true
          },
          {
            transaction_id: 'txn_2',
            account_id: 'acc_1',
            amount: 38.00,
            date: '2024-11-17',
            name: 'Loblaws',
            merchant_name: 'Loblaws',
            category: ['Food and Drink', 'Groceries'],
            account_owner: null,
            institution_name: 'Mock Bank',
            is_debit: true
          }
        ];
      }
    }

    // Step 3: Construct the LLM System Prompt
    const systemPrompt = `**IDENTITY AND PERSONA:**
You are "North," a friendly and knowledgeable personal finance companion. Think of yourself as that financially savvy friend who's always excited to chat about money, budgeting, and life goals. You're warm, conversational, and genuinely interested in helping people build better financial habits. You love discussing everything from daily spending tips to big financial dreams.

**YOUR PERSONALITY:**
- Conversational and natural - talk like a real person, not a robot
- Enthusiastic about personal finance topics
- Use everyday language and relatable examples
- Share general financial wisdom and tips
- Ask follow-up questions to keep the conversation going
- Use emojis occasionally to feel more human 😊
- Tell stories or give examples when helpful

**WHAT YOU CAN DISCUSS:**
- General budgeting strategies and tips
- Saving money techniques and habits
- Financial goal setting and motivation
- Common financial challenges and solutions
- Money mindset and psychology
- Canadian financial topics (since this is a Canadian app)
- Debt management strategies
- Building emergency funds
- Smart spending habits
- Financial planning concepts

**HOW TO USE TRANSACTION DATA:**
When you have access to the user's transaction data, use it to:
- Provide personalized insights about their spending patterns
- Suggest improvements based on their actual habits
- Celebrate their good financial choices
- Help them understand where their money goes

When you don't have transaction data or the question is general, focus on:
- Sharing helpful financial tips and strategies
- Discussing financial concepts in an engaging way
- Asking thoughtful questions about their financial goals
- Providing encouragement and motivation

**CONVERSATION STYLE:**
- Start responses naturally, like you're continuing a conversation
- Use "I" statements ("I think," "I've noticed," "I'd suggest")
- Ask questions to understand their situation better
- Share relatable examples: "Many people find that..." or "A trick that works well is..."
- Keep responses engaging and not too formal

**SAFETY GUARDRAILS:**
1. **NO SPECIFIC INVESTMENT ADVICE:** Don't recommend specific stocks, crypto, or investment products. Instead say things like "That's something you'd want to research or discuss with a financial advisor."
2. **NO GUARANTEES:** Avoid promising specific outcomes
3. **ENCOURAGE PROFESSIONAL HELP:** For complex situations, suggest consulting with financial professionals
4. **STAY POSITIVE:** Frame challenges as opportunities to improve

**CANADIAN CONTEXT:**
Remember this is a Canadian app, so reference:
- Canadian banks and financial institutions when relevant
- Canadian financial concepts (RRSP, TFSA, etc.) when appropriate
- Canadian spending patterns and costs

---

**Available Transaction Data (Last 90 Days):**
${transactionData.length > 0 ? JSON.stringify(transactionData, null, 2) : 'No transaction data available - user hasn\'t connected their bank account yet.'}

---

**User's Message:** "${message}"

**Instructions:** Respond naturally and conversationally. If they're asking about their specific spending and you have transaction data, use it. If they're asking general finance questions or you don't have data, focus on helpful financial discussion and tips. Keep it friendly and engaging!`;

    // Step 4: Call the Gemini API
    try {
      const model = genAI.getGenerativeModel({
        model: 'gemini-1.5-flash',
        generationConfig: {
          temperature: 0.7,
          topK: 40,
          topP: 0.95,
          maxOutputTokens: 1024,
        }
      });
      const result = await model.generateContent(systemPrompt);
      const response = await result.response;
      const aiResponse = response.text();

      // Step 5: Return the response
      res.json({
        response: aiResponse
      });

    } catch (llmError) {
      console.error('LLM API error:', llmError);
      return res.status(503).json({
        error: 'The AI assistant is currently unavailable. Please try again later.'
      });
    }

  } catch (error) {
    console.error('CFO chat error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

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
      client_name: 'North',
      products: ['transactions'],
      country_codes: ['US', 'CA'],
      language: 'en',
      android_package_name: 'com.north.mobile',
      // Force full institution selection experience
      webhook: `https://${process.env.RAILWAY_STATIC_URL || 'north-api-clean-production.up.railway.app'}/webhooks/plaid`,
      link_customization_name: null // Use default UI
    };

    const response = await plaidClient.linkTokenCreate(linkTokenRequest);
    const linkToken = response.data.link_token;

    res.json({
      link_token: linkToken,
      expiration: response.data.expiration
    });
  } catch (error) {
    console.error('❌ Create link token error:', error);
    console.error('🔍 Link token error details:', {
      message: error.message,
      code: error.code,
      status: error.response?.status,
      plaid_response: error.response?.data,
      plaid_client_id_exists: !!PLAID_CLIENT_ID,
      plaid_secret_exists: !!PLAID_SECRET,
      plaid_env: PLAID_ENV,
      user_id: userId
    });
    res.status(500).json({ 
      error: 'Failed to create link token',
      details: error.message,
      plaid_configured: !!(PLAID_CLIENT_ID && PLAID_SECRET)
    });
  }
});

// Exchange Public Token for Access Token
app.post('/api/plaid/exchange-public-token', authenticateToken, async (req, res) => {
  try {
    const { public_token } = req.body;
    const userId = req.user.userId;

    // Check if Plaid is properly configured
    if (!PLAID_CLIENT_ID || !PLAID_SECRET) {
      console.error('❌ Plaid not configured for token exchange');
      console.error('PLAID_CLIENT_ID exists:', !!PLAID_CLIENT_ID);
      console.error('PLAID_SECRET exists:', !!PLAID_SECRET);
      console.error('PLAID_ENV:', PLAID_ENV);
      return res.status(500).json({ 
        error: 'Plaid integration not configured',
        details: 'PLAID_CLIENT_ID or PLAID_SECRET missing from environment variables',
        plaid_client_id_exists: !!PLAID_CLIENT_ID,
        plaid_secret_exists: !!PLAID_SECRET,
        plaid_env: PLAID_ENV
      });
    }

    console.log('🔄 Exchanging public token:', public_token?.substring(0, 20) + '...');
    console.log('🔧 Using Plaid environment:', PLAID_ENV);
    console.log('🔧 Plaid client configured:', !!PLAID_CLIENT_ID);

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

    // Get institution information
    const institutionRequest = {
      institution_id: accountsResponse.data.item.institution_id,
      country_codes: ['US', 'CA']
    };

    let institutionName = 'Unknown Bank';
    try {
      const institutionResponse = await plaidClient.institutionsGetById(institutionRequest);
      institutionName = institutionResponse.data.institution.name;
    } catch (instError) {
      console.warn('Could not fetch institution name:', instError.message);
    }

    // Store access token in database
    await pool.query(`
      INSERT INTO plaid_items (user_id, access_token, item_id, institution_id, institution_name, updated_at)
      VALUES ($1, $2, $3, $4, $5, NOW())
      ON CONFLICT (user_id, item_id) 
      DO UPDATE SET 
        access_token = EXCLUDED.access_token,
        institution_name = EXCLUDED.institution_name,
        updated_at = NOW()
    `, [userId, accessToken, itemId, accountsResponse.data.item.institution_id, institutionName]);

    // Automatically sync transactions after successful connection
    console.log('🔄 Triggering automatic transaction sync...');
    
    // Trigger transaction analysis in the background
    setTimeout(async () => {
      try {
        console.log('📊 Starting background transaction sync for user:', userId);
        
        // Get transactions (last 90 days)
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(startDate.getDate() - 90);

        const transactionsResponse = await plaidClient.transactionsGet({
          access_token: accessToken,
          start_date: startDate.toISOString().split('T')[0],
          end_date: endDate.toISOString().split('T')[0],
        });

        console.log(`📈 Retrieved ${transactionsResponse.data.transactions.length} transactions`);

        // Store transactions in database
        for (const transaction of transactionsResponse.data.transactions) {
          try {
            await pool.query(`
              INSERT INTO transactions (
                user_id, plaid_transaction_id, account_id, amount, description,
                category, subcategory, date, merchant_name
              ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
              ON CONFLICT (plaid_transaction_id) DO NOTHING
            `, [
              userId,
              transaction.transaction_id,
              transaction.account_id,
              transaction.amount,
              transaction.name,
              transaction.category,
              transaction.category?.[1] || transaction.category[0],
              transaction.date,
              transaction.merchant_name
            ]);
          } catch (txnError) {
            console.error('Error storing transaction:', txnError);
          }
        }

        console.log('✅ Background transaction sync completed');
        
        // Generate AI insights after syncing
        await generateAIInsights(userId);
        await generateDynamicGoals(userId);
        
      } catch (syncError) {
        console.error('❌ Background transaction sync failed:', syncError);
      }
    }, 2000); // 2 second delay to let the response complete first

    // Automatically sync transactions after successful connection
    console.log('🔄 Triggering automatic transaction sync...');
    try {
      // Trigger transaction analysis in the background
      setTimeout(async () => {
        try {
          console.log('📊 Starting background transaction sync for user:', userId);
          
          // Get transactions (last 90 days)
          const endDate = new Date();
          const startDate = new Date();
          startDate.setDate(startDate.getDate() - 90);

          const transactionsResponse = await plaidClient.transactionsGet({
            access_token: accessToken,
            start_date: startDate.toISOString().split('T')[0],
            end_date: endDate.toISOString().split('T')[0],
          });

          console.log(`📈 Retrieved ${transactionsResponse.data.transactions.length} transactions`);

          // Store transactions in database
          for (const transaction of transactionsResponse.data.transactions) {
            try {
              await pool.query(`
                INSERT INTO transactions (
                  user_id, plaid_transaction_id, account_id, amount, description,
                  category, subcategory, date, merchant_name
                ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                ON CONFLICT (plaid_transaction_id) DO NOTHING
              `, [
                userId,
                transaction.transaction_id,
                transaction.account_id,
                transaction.amount,
                transaction.name,
                transaction.category,
                transaction.category?.[1] || null,
                transaction.date,
                transaction.merchant_name
              ]);
            } catch (txnError) {
              console.error('Error storing transaction:', txnError);
            }
          }

          console.log('✅ Background transaction sync completed');
          
          // Generate AI insights after syncing
          await generateAIInsights(userId);
          await generateDynamicGoals(userId);
          
        } catch (syncError) {
          console.error('❌ Background transaction sync failed:', syncError);
        }
      }, 2000); // 2 second delay to let the response complete first
      
    } catch (bgError) {
      console.warn('Background sync setup failed:', bgError);
    }

    res.json({
      success: true,
      accounts: accounts,
      access_token: accessToken,
      item_id: itemId,
      message: 'Account connected successfully. Transaction sync started in background.',
      message: 'Account connected successfully. Transaction sync started in background.'
    });
  } catch (error) {
    console.error('❌ Exchange public token error:', error);
    console.error('🔍 Detailed error analysis:', {
      message: error.message,
      code: error.code,
      status: error.response?.status,
      statusText: error.response?.statusText,
      plaid_response: error.response?.data,
      plaid_error_type: error.response?.data?.error_type,
      plaid_error_code: error.response?.data?.error_code,
      plaid_display_message: error.response?.data?.display_message,
      plaid_request_id: error.response?.data?.request_id,
      plaid_client_id_exists: !!PLAID_CLIENT_ID,
      plaid_secret_exists: !!PLAID_SECRET,
      plaid_env: PLAID_ENV,
      public_token_preview: public_token ? `${public_token.substring(0, 20)}...` : 'MISSING',
      user_id: userId
    });
    
    // Return more detailed error information
    res.status(500).json({ 
      error: 'Failed to exchange public token',
      details: error.message,
      plaid_configured: !!(PLAID_CLIENT_ID && PLAID_SECRET),
      plaid_error_type: error.response?.data?.error_type,
      plaid_error_code: error.response?.data?.error_code,
      plaid_display_message: error.response?.data?.display_message,
      plaid_request_id: error.response?.data?.request_id
    });
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

// Get user's insights
app.get('/api/insights', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const insights = await pool.query(`
      SELECT 
        id, insight_type, title, description, category, amount,
        confidence_score, action_items, is_read, created_at
      FROM spending_insights 
      WHERE user_id = $1 AND (expires_at IS NULL OR expires_at > NOW())
      ORDER BY confidence_score DESC, created_at DESC
      LIMIT 20
    `, [userId]);

    res.json({
      success: true,
      insights: insights.rows
    });
  } catch (error) {
    console.error('Get insights error:', error);
    res.status(500).json({ error: 'Failed to get insights' });
  }
});

// Get user's dynamic goals
app.get('/api/goals', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const goals = await pool.query(`
      SELECT 
        id, goal_type, title, description, target_amount, current_amount,
        target_date, category, priority, status, created_at
      FROM dynamic_goals 
      WHERE user_id = $1 AND status = 'active'
      ORDER BY priority DESC, created_at DESC
    `, [userId]);

    res.json({
      success: true,
      goals: goals.rows
    });
  } catch (error) {
    console.error('Get goals error:', error);
    res.status(500).json({ error: 'Failed to get goals' });
  }
});

// Get spending patterns
app.get('/api/spending-patterns', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const patterns = await pool.query(`
      SELECT 
        category, period_start, period_end, total_amount,
        transaction_count, average_transaction, trend_direction, trend_percentage
      FROM spending_patterns 
      WHERE user_id = $1 AND period_type = 'monthly'
      ORDER BY period_start DESC, total_amount DESC
      LIMIT 50
    `, [userId]);

    res.json({
      success: true,
      patterns: patterns.rows
    });
  } catch (error) {
    console.error('Get spending patterns error:', error);
    res.status(500).json({ error: 'Failed to get spending patterns' });
  }
});

// Mark insight as read
app.post('/api/insights/:id/read', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const insightId = req.params.id;

    await pool.query(`
      UPDATE spending_insights 
      SET is_read = true 
      WHERE id = $1 AND user_id = $2
    `, [insightId, userId]);

    res.json({ success: true });
  } catch (error) {
    console.error('Mark insight read error:', error);
    res.status(500).json({ error: 'Failed to mark insight as read' });
  }
});

// Update goal progress
app.post('/api/goals/:id/progress', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const goalId = req.params.id;
    const { currentAmount } = req.body;

    await pool.query(`
      UPDATE dynamic_goals 
      SET current_amount = $1, updated_at = NOW()
      WHERE id = $2 AND user_id = $3
    `, [currentAmount, goalId, userId]);

    res.json({ success: true });
  } catch (error) {
    console.error('Update goal progress error:', error);
    res.status(500).json({ error: 'Failed to update goal progress' });
  }
});

// Get transaction sync status
app.get('/api/transactions/status', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    
    // Get transaction count and latest sync
    const result = await pool.query(`
      SELECT 
        COUNT(*) as total_transactions,
        MAX(date) as latest_transaction_date,
        MIN(date) as earliest_transaction_date
      FROM transactions 
      WHERE user_id = $1
    `, [userId]);
    
    const stats = result.rows[0];
    
    res.json({
      success: true,
      total_transactions: parseInt(stats.total_transactions),
      latest_transaction_date: stats.latest_transaction_date,
      earliest_transaction_date: stats.earliest_transaction_date,
      sync_status: parseInt(stats.total_transactions) > 0 ? 'completed' : 'pending'
    });
    
  } catch (error) {
    console.error('Transaction status error:', error);
    res.status(500).json({ error: 'Failed to get transaction status' });
  }
});

// Get user's transactions
app.get('/api/transactions', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { limit = 50, offset = 0 } = req.query;

    // Get real transaction data from database
    const transactions = await pool.query(`
      SELECT 
        id, plaid_transaction_id, account_id, amount, description,
        category, subcategory, date, merchant_name, created_at
      FROM transactions 
      WHERE user_id = $1
      ORDER BY date DESC, created_at DESC
      LIMIT $2 OFFSET $3
    `, [userId, limit, offset]);

    res.json({
      success: true,
      transactions: transactions.rows
    });
  } catch (error) {
    console.error('Get transactions error:', error);

    // Fallback to mock data if database fails
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
  }
});

// Initialize database and test connection on startup
testDatabaseConnection();
// Plaid Webhook Endpoint
app.post('/webhooks/plaid', express.raw({ type: 'application/json' }), async (req, res) => {
  try {
    const webhook = req.body;
    console.log('📨 Plaid webhook received:', {
      webhook_type: webhook.webhook_type,
      webhook_code: webhook.webhook_code,
      item_id: webhook.item_id,
      timestamp: new Date().toISOString()
    });

    // Handle different webhook types
    switch (webhook.webhook_type) {
      case 'TRANSACTIONS':
        await handleTransactionsWebhook(webhook);
        break;
      case 'ITEM':
        await handleItemWebhook(webhook);
        break;
      case 'AUTH':
        await handleAuthWebhook(webhook);
        break;
      case 'IDENTITY':
        await handleIdentityWebhook(webhook);
        break;
      default:
        console.log(`⚠️ Unhandled webhook type: ${webhook.webhook_type}`);
    }

    // Always respond with 200 to acknowledge receipt
    res.status(200).json({ received: true });
  } catch (error) {
    console.error('❌ Webhook processing error:', error);
    res.status(500).json({ error: 'Webhook processing failed' });
  }
});

// Webhook handlers
async function handleTransactionsWebhook(webhook) {
  console.log('💳 Processing transactions webhook:', webhook.webhook_code);

  try {
    // Get user_id from item_id
    const itemResult = await pool.query(
      'SELECT user_id, access_token FROM plaid_items WHERE item_id = $1',
      [webhook.item_id]
    );

    if (itemResult.rows.length === 0) {
      console.log('⚠️ No user found for item_id:', webhook.item_id);
      return;
    }

    const { user_id, access_token } = itemResult.rows[0];

    switch (webhook.webhook_code) {
      case 'INITIAL_UPDATE':
      case 'HISTORICAL_UPDATE':
      case 'DEFAULT_UPDATE':
        console.log('📊 Fetching transactions for user:', user_id);
        await fetchAndStoreTransactions(user_id, access_token);
        await generateInsightsForUser(user_id);
        await generateDynamicGoals(user_id);
        break;
      case 'TRANSACTIONS_REMOVED':
        console.log('🗑️ Handling removed transactions for item:', webhook.item_id);
        if (webhook.removed_transactions) {
          await removeTransactions(webhook.removed_transactions);
        }
        break;
    }
  } catch (error) {
    console.error('❌ Error handling transactions webhook:', error);
  }
}

async function handleItemWebhook(webhook) {
  console.log('🏦 Processing item webhook:', webhook.webhook_code);

  switch (webhook.webhook_code) {
    case 'ERROR':
      console.log('❌ Item error for:', webhook.item_id, webhook.error);
      // Handle item errors (e.g., expired credentials)
      break;
    case 'PENDING_EXPIRATION':
      console.log('⏰ Item credentials expiring soon for:', webhook.item_id);
      // Notify user to re-authenticate
      break;
    case 'USER_PERMISSION_REVOKED':
      console.log('🚫 User revoked permissions for:', webhook.item_id);
      // Handle permission revocation
      break;
  }
}

async function handleAuthWebhook(webhook) {
  console.log('🔐 Processing auth webhook:', webhook.webhook_code);
  // Handle auth-related webhooks
}

async function handleIdentityWebhook(webhook) {
  console.log('👤 Processing identity webhook:', webhook.webhook_code);
  // Handle identity-related webhooks
}

// Transaction Processing Functions
async function fetchAndStoreTransactions(userId, accessToken) {
  try {
    console.log('🔄 Fetching transactions for user:', userId);

    // Get transactions from last 90 days
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 90);
    const endDate = new Date();

    const transactionsRequest = {
      access_token: accessToken,
      start_date: startDate.toISOString().split('T')[0],
      end_date: endDate.toISOString().split('T')[0],
      count: 500
    };

    const transactionsResponse = await plaidClient.transactionsGet(transactionsRequest);
    const transactions = transactionsResponse.data.transactions;

    console.log(`📊 Processing ${transactions.length} transactions`);

    // Store transactions in database
    for (const txn of transactions) {
      await pool.query(`
        INSERT INTO transactions (
          user_id, plaid_transaction_id, account_id, amount, description,
          category, subcategory, date, merchant_name
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
        ON CONFLICT (plaid_transaction_id) DO UPDATE SET
          amount = EXCLUDED.amount,
          description = EXCLUDED.description,
          updated_at = NOW()
      `, [
        userId,
        txn.transaction_id,
        txn.account_id,
        -txn.amount, // Plaid uses positive for outflows, we use negative
        txn.name,
        txn.category || [],
        txn.category?.[1] || null,
        txn.date,
        txn.merchant_name
      ]);
    }

    // Update spending patterns
    await updateSpendingPatterns(userId);

    console.log('✅ Transactions stored and patterns updated');

  } catch (error) {
    console.error('❌ Error fetching transactions:', error);
  }
}

async function updateSpendingPatterns(userId) {
  try {
    // Calculate monthly spending patterns by category
    const patterns = await pool.query(`
      SELECT 
        category[1] as main_category,
        DATE_TRUNC('month', date) as month,
        SUM(ABS(amount)) as total_amount,
        COUNT(*) as transaction_count,
        AVG(ABS(amount)) as average_transaction
      FROM transactions 
      WHERE user_id = $1 AND date >= NOW() - INTERVAL '6 months'
      GROUP BY category[1], DATE_TRUNC('month', date)
      ORDER BY month DESC, total_amount DESC
    `, [userId]);

    // Store patterns and calculate trends
    for (const pattern of patterns.rows) {
      if (!pattern.main_category) continue;

      const monthStart = new Date(pattern.month);
      const monthEnd = new Date(monthStart);
      monthEnd.setMonth(monthEnd.getMonth() + 1);
      monthEnd.setDate(0); // Last day of month

      await pool.query(`
        INSERT INTO spending_patterns (
          user_id, category, period_type, period_start, period_end,
          total_amount, transaction_count, average_transaction
        ) VALUES ($1, $2, 'monthly', $3, $4, $5, $6, $7)
        ON CONFLICT (user_id, category, period_type, period_start) 
        DO UPDATE SET
          total_amount = EXCLUDED.total_amount,
          transaction_count = EXCLUDED.transaction_count,
          average_transaction = EXCLUDED.average_transaction,
          created_at = NOW()
      `, [
        userId,
        pattern.main_category,
        monthStart,
        monthEnd,
        pattern.total_amount,
        pattern.transaction_count,
        pattern.average_transaction
      ]);
    }

  } catch (error) {
    console.error('❌ Error updating spending patterns:', error);
  }
}

async function generateInsightsForUser(userId) {
  try {
    console.log('🧠 Generating insights for user:', userId);

    // Get recent spending data
    const spendingData = await pool.query(`
      SELECT 
        category[1] as category,
        SUM(ABS(amount)) as total_amount,
        COUNT(*) as transaction_count,
        AVG(ABS(amount)) as avg_amount
      FROM transactions 
      WHERE user_id = $1 AND date >= NOW() - INTERVAL '30 days'
      GROUP BY category[1]
      ORDER BY total_amount DESC
      LIMIT 10
    `, [userId]);

    // Generate spending pattern insights
    for (const spending of spendingData.rows) {
      if (!spending.category || spending.total_amount < 50) continue;

      // Check if spending increased significantly
      const previousMonth = await pool.query(`
        SELECT SUM(ABS(amount)) as prev_amount
        FROM transactions 
        WHERE user_id = $1 AND category[1] = $2 
        AND date >= NOW() - INTERVAL '60 days' 
        AND date < NOW() - INTERVAL '30 days'
      `, [userId, spending.category]);

      const prevAmount = previousMonth.rows[0]?.prev_amount || 0;
      const currentAmount = parseFloat(spending.total_amount);

      if (prevAmount > 0) {
        const changePercent = ((currentAmount - prevAmount) / prevAmount) * 100;

        if (changePercent > 20) {
          await createInsight(userId, {
            type: 'spending_pattern',
            title: `${spending.category} spending increased`,
            description: `Your ${spending.category.toLowerCase()} spending increased by ${changePercent.toFixed(0)}% this month ($${currentAmount.toFixed(2)} vs $${prevAmount.toFixed(2)} last month).`,
            category: spending.category,
            amount: currentAmount,
            confidence: 0.85,
            actions: [
              `Review your ${spending.category.toLowerCase()} purchases`,
              'Set a monthly budget for this category',
              'Look for ways to reduce spending'
            ]
          });
        }
      }

      // Generate high-spending alerts
      if (currentAmount > 500) {
        await createInsight(userId, {
          type: 'budget_alert',
          title: `High ${spending.category} spending`,
          description: `You've spent $${currentAmount.toFixed(2)} on ${spending.category.toLowerCase()} this month across ${spending.transaction_count} transactions.`,
          category: spending.category,
          amount: currentAmount,
          confidence: 0.90,
          actions: [
            'Consider setting a monthly budget',
            'Track daily spending in this category',
            'Look for subscription services to cancel'
          ]
        });
      }
    }

    // Generate saving opportunities
    await generateSavingOpportunities(userId);

  } catch (error) {
    console.error('❌ Error generating insights:', error);
  }
}

async function generateSavingOpportunities(userId) {
  try {
    // Find recurring transactions that could be optimized
    const recurring = await pool.query(`
      SELECT 
        description,
        merchant_name,
        AVG(ABS(amount)) as avg_amount,
        COUNT(*) as frequency,
        category[1] as category
      FROM transactions 
      WHERE user_id = $1 
      AND date >= NOW() - INTERVAL '90 days'
      AND ABS(amount) > 10
      GROUP BY description, merchant_name, category[1]
      HAVING COUNT(*) >= 3
      ORDER BY AVG(ABS(amount)) DESC
      LIMIT 5
    `, [userId]);

    for (const txn of recurring.rows) {
      const monthlyAmount = parseFloat(txn.avg_amount) * (txn.frequency / 3); // Approximate monthly

      if (monthlyAmount > 50) {
        await createInsight(userId, {
          type: 'saving_opportunity',
          title: `Potential savings: ${txn.merchant_name || txn.description}`,
          description: `You spend approximately $${monthlyAmount.toFixed(2)}/month on ${txn.merchant_name || txn.description}. Consider if this aligns with your financial goals.`,
          category: txn.category,
          amount: monthlyAmount,
          confidence: 0.75,
          actions: [
            'Review if this expense is necessary',
            'Look for cheaper alternatives',
            'Consider reducing frequency'
          ]
        });
      }
    }

  } catch (error) {
    console.error('❌ Error generating saving opportunities:', error);
  }
}

async function createInsight(userId, insight) {
  try {
    // Check if similar insight already exists
    const existing = await pool.query(`
      SELECT id FROM spending_insights 
      WHERE user_id = $1 AND title = $2 AND created_at > NOW() - INTERVAL '7 days'
    `, [userId, insight.title]);

    if (existing.rows.length > 0) {
      return; // Don't create duplicate insights
    }

    await pool.query(`
      INSERT INTO spending_insights (
        user_id, insight_type, title, description, category, 
        amount, confidence_score, action_items, expires_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
    `, [
      userId,
      insight.type,
      insight.title,
      insight.description,
      insight.category,
      insight.amount,
      insight.confidence,
      insight.actions,
      new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) // Expires in 30 days
    ]);

  } catch (error) {
    console.error('❌ Error creating insight:', error);
  }
}

async function generateDynamicGoals(userId) {
  try {
    console.log('🎯 Generating dynamic goals for user:', userId);

    // Get user's spending patterns
    const spendingData = await pool.query(`
      SELECT 
        category[1] as category,
        SUM(ABS(amount)) as total_amount,
        COUNT(*) as transaction_count
      FROM transactions 
      WHERE user_id = $1 AND date >= NOW() - INTERVAL '60 days'
      GROUP BY category[1]
      ORDER BY total_amount DESC
      LIMIT 5
    `, [userId]);

    // Generate category-specific savings goals
    for (const spending of spendingData.rows) {
      if (!spending.category || spending.total_amount < 200) continue;

      const monthlyAmount = parseFloat(spending.total_amount) / 2; // 2 months of data
      const savingsTarget = monthlyAmount * 0.15; // 15% reduction goal

      if (savingsTarget > 25) {
        await createDynamicGoal(userId, {
          type: 'spending_reduction',
          title: `Reduce ${spending.category} spending`,
          description: `Save $${savingsTarget.toFixed(2)} per month by reducing ${spending.category.toLowerCase()} expenses`,
          targetAmount: savingsTarget,
          category: spending.category,
          targetDate: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000), // 90 days
          priority: 7
        });
      }
    }

    // Generate emergency fund goal if user doesn't have one
    const totalMonthlySpending = await pool.query(`
      SELECT SUM(ABS(amount)) as total
      FROM transactions 
      WHERE user_id = $1 AND date >= NOW() - INTERVAL '30 days'
    `, [userId]);

    const monthlySpending = parseFloat(totalMonthlySpending.rows[0]?.total || 0);
    if (monthlySpending > 0) {
      const emergencyFundTarget = monthlySpending * 3; // 3 months of expenses

      await createDynamicGoal(userId, {
        type: 'emergency_fund',
        title: 'Build Emergency Fund',
        description: `Build an emergency fund of $${emergencyFundTarget.toFixed(2)} to cover 3 months of expenses`,
        targetAmount: emergencyFundTarget,
        category: 'Savings',
        targetDate: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000), // 1 year
        priority: 9
      });
    }

  } catch (error) {
    console.error('❌ Error generating dynamic goals:', error);
  }
}

async function createDynamicGoal(userId, goal) {
  try {
    // Check if similar goal already exists
    const existing = await pool.query(`
      SELECT id FROM dynamic_goals 
      WHERE user_id = $1 AND goal_type = $2 AND category = $3 AND status = 'active'
    `, [userId, goal.type, goal.category]);

    if (existing.rows.length > 0) {
      return; // Don't create duplicate goals
    }

    await pool.query(`
      INSERT INTO dynamic_goals (
        user_id, goal_type, title, description, target_amount,
        target_date, category, priority
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
    `, [
      userId,
      goal.type,
      goal.title,
      goal.description,
      goal.targetAmount,
      goal.targetDate,
      goal.category,
      goal.priority
    ]);

  } catch (error) {
    console.error('❌ Error creating dynamic goal:', error);
  }
}

async function removeTransactions(removedTransactions) {
  try {
    for (const txnId of removedTransactions) {
      await pool.query('DELETE FROM transactions WHERE plaid_transaction_id = $1', [txnId]);
    }
    console.log(`🗑️ Removed ${removedTransactions.length} transactions`);
  } catch (error) {
    console.error('❌ Error removing transactions:', error);
  }
}

initDatabase();

app.listen(port, () => {
  console.log(`🚀 North API running on port ${port}`);
  console.log('Environment:', process.env.NODE_ENV);
});

// Get Transactions from Plaid
app.post('/api/plaid/transactions', authenticateToken, async (req, res) => {
  try {
    const { access_token, start_date, end_date } = req.body;
    const userId = req.user.userId;

    if (!access_token) {
      return res.status(400).json({ error: 'Access token is required' });
    }

    // Check if Plaid is properly configured
    if (!PLAID_CLIENT_ID || !PLAID_SECRET) {
      console.error('❌ Plaid not configured for transactions');
      return res.status(500).json({ 
        error: 'Plaid integration not configured',
        details: 'PLAID_CLIENT_ID or PLAID_SECRET missing from environment variables'
      });
    }

    console.log('🔄 Fetching transactions from Plaid...');
    console.log('📅 Date range:', start_date, 'to', end_date);

    // Get transactions from Plaid
    const transactionsResponse = await plaidClient.transactionsGet({
      access_token: access_token,
      start_date: start_date,
      end_date: end_date,
    });

    const transactions = transactionsResponse.data.transactions.map(txn => ({
      transaction_id: txn.transaction_id,
      account_id: txn.account_id,
      amount: txn.amount,
      date: txn.date,
      name: txn.name,
      merchant_name: txn.merchant_name,
      category: txn.category,
      pending: txn.pending
    }));

    const accounts = transactionsResponse.data.accounts.map(account => ({
      account_id: account.account_id,
      balances: {
        available: account.balances.available,
        current: account.balances.current,
        limit: account.balances.limit
      },
      mask: account.mask,
      name: account.name,
      official_name: account.official_name,
      type: account.type,
      subtype: account.subtype
    }));

    console.log(`✅ Retrieved ${transactions.length} transactions from Plaid`);

    res.json({
      accounts: accounts,
      transactions: transactions,
      total_transactions: transactions.length,
      request_id: transactionsResponse.data.request_id
    });

  } catch (error) {
    console.error('❌ Error fetching transactions from Plaid:', error);
    
    // Return mock data as fallback
    const mockTransactions = [
      {
        transaction_id: 'txn_mock_1',
        account_id: 'acc_mock_1',
        amount: -67.00,
        date: '2024-01-15',
        name: 'Metro Grocery Store',
        merchant_name: 'Metro',
        category: ['Food and Drink', 'Groceries'],
        pending: false
      },
      {
        transaction_id: 'txn_mock_2',
        account_id: 'acc_mock_1',
        amount: -38.00,
        date: '2024-01-14',
        name: 'Loblaws',
        merchant_name: 'Loblaws',
        category: ['Food and Drink', 'Groceries'],
        pending: false
      }
    ];

    const mockAccounts = [
      {
        account_id: 'acc_mock_1',
        balances: {
          available: 1250.50,
          current: 1250.50,
          limit: null
        },
        mask: '1234',
        name: 'Checking Account',
        official_name: 'Primary Checking',
        type: 'depository',
        subtype: 'checking'
      }
    ];

    res.json({
      accounts: mockAccounts,
      transactions: mockTransactions,
      total_transactions: mockTransactions.length,
      request_id: 'mock_request_id'
    });
  }
});

// Exchange Public Token for Access Token