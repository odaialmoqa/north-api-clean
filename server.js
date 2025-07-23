const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

// Only load .env file in development
if (process.env.NODE_ENV !== 'production') {
  require('dotenv').config();
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

// Forgot password endpoint
app.post('/api/auth/forgot-password', async (req, res) => {
  try {
    const { email } = req.body;
    
    if (!email) {
      return res.status(400).json({ error: 'Email is required' });
    }
    
    // Check if user exists
    const result = await pool.query('SELECT id, email, first_name FROM users WHERE email = $1', [email]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Email address not found' });
    }
    
    const user = result.rows[0];
    
    // Generate password reset token (valid for 1 hour)
    const resetToken = jwt.sign(
      { userId: user.id, email: user.email, type: 'password_reset' }, 
      process.env.JWT_SECRET, 
      { expiresIn: '1h' }
    );
    
    // In a real application, you would:
    // 1. Store the reset token in the database with expiration
    // 2. Send an email with the reset link
    // 3. The reset link would be something like: https://yourapp.com/reset-password?token=resetToken
    
    console.log(`Password reset requested for: ${email}`);
    console.log(`Reset token (for testing): ${resetToken}`);
    
    // For now, we'll just return success
    // In production, you wouldn't reveal whether the email exists or not for security
    res.json({
      message: `Password reset link sent to ${email}`,
      success: true
    });
    
  } catch (error) {
    console.error('Forgot password error:', error);
    res.status(500).json({ error: 'Password reset request failed' });
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

// AI Chat endpoint
app.post('/api/ai/chat', authenticateToken, async (req, res) => {
  try {
    const { message, context } = req.body;
    const userId = req.user.userId;
    
    // Mock AI response - replace with real AI service integration
    const aiResponse = {
      message: `Hey there! I understand you're asking about: "${message}". Based on your financial data, here's what I think...`,
      tone: 'friendly',
      supportingData: [
        'Your current net worth is $124,500',
        'You\'re on track with your emergency fund goal'
      ],
      recommendations: [
        'Consider increasing your savings rate by 5%',
        'You have room in your entertainment budget'
      ],
      followUpQuestions: [
        'Would you like to know more about your spending patterns?',
        'Should we look at optimizing your budget?'
      ]
    };
    
    res.json(aiResponse);
  } catch (error) {
    console.error('AI chat error:', error);
    res.status(500).json({ error: 'AI chat failed' });
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