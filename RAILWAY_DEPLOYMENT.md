# Railway + Custom Auth Deployment Guide

## 🚀 Why Custom Auth for North?

### Benefits Over Firebase
- ✅ **PIPEDA Compliant** - Data stays in Canada/North America
- ✅ **No vendor lock-in** - You own your user data
- ✅ **Cost effective** - No per-user fees
- ✅ **Customizable** - Add biometric auth, Canadian banking features
- ✅ **Simple** - One database, one service

## Step 1: Railway Setup (5 minutes)

### Create Railway Project
```bash
# Install Railway CLI
npm install -g @railway/cli

# Login with GitHub
railway login

# Create new project
mkdir north-backend
cd north-backend
railway init
```

### Add PostgreSQL Database
```bash
# Add PostgreSQL service
railway add postgresql

# Get your database URL
railway variables
# Copy the DATABASE_URL - you'll need this
```

## Step 2: Backend Structure

### Project Structure
```
north-backend/
├── package.json
├── server.js
├── routes/
│   ├── auth.js
│   ├── users.js
│   ├── accounts.js
│   └── goals.js
├── middleware/
│   ├── auth.js
│   └── validation.js
├── models/
│   └── User.js
├── database/
│   └── init.sql
└── utils/
    └── jwt.js
```

### Package.json
```json
{
  "name": "north-backend",
  "version": "1.0.0",
  "description": "North Financial App Backend",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "pg": "^8.11.3",
    "bcryptjs": "^2.4.3",
    "jsonwebtoken": "^9.0.2",
    "joi": "^17.11.0",
    "helmet": "^7.1.0",
    "express-rate-limit": "^7.1.5",
    "dotenv": "^16.3.1"
  },
  "devDependencies": {
    "nodemon": "^3.0.2"
  }
}
```

## Step 3: Database Schema

### database/init.sql
```sql
-- Users table with Canadian-specific fields
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    province VARCHAR(2), -- Canadian province code
    postal_code VARCHAR(7), -- Canadian postal code format
    date_of_birth DATE,
    sin_hash VARCHAR(255), -- Hashed SIN for verification
    is_verified BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    biometric_enabled BOOLEAN DEFAULT FALSE,
    pin_hash VARCHAR(255),
    last_login TIMESTAMP,
    login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP -- Soft delete for PIPEDA compliance
);

-- User sessions for JWT management
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255),
    device_info JSONB,
    ip_address INET,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    plaid_account_id VARCHAR(255),
    account_name VARCHAR(255),
    account_type VARCHAR(50), -- checking, savings, credit, rrsp, tfsa
    account_subtype VARCHAR(50),
    balance DECIMAL(12,2),
    available_balance DECIMAL(12,2),
    currency VARCHAR(3) DEFAULT 'CAD',
    institution_name VARCHAR(255),
    institution_id VARCHAR(255),
    mask VARCHAR(10), -- Last 4 digits
    is_active BOOLEAN DEFAULT TRUE,
    last_synced TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Financial goals
CREATE TABLE IF NOT EXISTS financial_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_amount DECIMAL(12,2) NOT NULL,
    current_amount DECIMAL(12,2) DEFAULT 0,
    target_date DATE,
    priority INTEGER DEFAULT 1,
    category VARCHAR(50), -- emergency, vacation, retirement, etc.
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Transactions
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID REFERENCES accounts(id) ON DELETE CASCADE,
    plaid_transaction_id VARCHAR(255),
    amount DECIMAL(12,2) NOT NULL,
    description TEXT,
    merchant_name VARCHAR(255),
    category VARCHAR(100),
    subcategory VARCHAR(100),
    date DATE NOT NULL,
    pending BOOLEAN DEFAULT FALSE,
    location JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Audit log for PIPEDA compliance
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id UUID,
    details JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(date);
CREATE INDEX IF NOT EXISTS idx_goals_user_id ON financial_goals(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
```

## Step 4: Authentication Implementation

### utils/jwt.js
```javascript
const jwt = require('jsonwebtoken');
const crypto = require('crypto');

const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-key-change-this';
const JWT_EXPIRES_IN = '15m'; // Short-lived access tokens
const REFRESH_TOKEN_EXPIRES_IN = '7d';

class JWTUtils {
  static generateTokens(payload) {
    const accessToken = jwt.sign(payload, JWT_SECRET, { 
      expiresIn: JWT_EXPIRES_IN 
    });
    
    const refreshToken = crypto.randomBytes(40).toString('hex');
    
    return { accessToken, refreshToken };
  }
  
  static verifyAccessToken(token) {
    try {
      return jwt.verify(token, JWT_SECRET);
    } catch (error) {
      throw new Error('Invalid or expired token');
    }
  }
  
  static hashRefreshToken(token) {
    return crypto.createHash('sha256').update(token).digest('hex');
  }
}

module.exports = JWTUtils;
```

### middleware/auth.js
```javascript
const JWTUtils = require('../utils/jwt');
const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

const authenticateToken = async (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN
  
  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }
  
  try {
    const decoded = JWTUtils.verifyAccessToken(token);
    
    // Check if user still exists and is active
    const userResult = await pool.query(
      'SELECT id, email, is_verified, deleted_at FROM users WHERE id = $1',
      [decoded.userId]
    );
    
    if (userResult.rows.length === 0 || userResult.rows[0].deleted_at) {
      return res.status(401).json({ error: 'User not found or deactivated' });
    }
    
    req.user = userResult.rows[0];
    next();
  } catch (error) {
    return res.status(403).json({ error: 'Invalid or expired token' });
  }
};

module.exports = { authenticateToken };
```

### routes/auth.js
```javascript
const express = require('express');
const bcrypt = require('bcryptjs');
const Joi = require('joi');
const { Pool } = require('pg');
const JWTUtils = require('../utils/jwt');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

// Validation schemas
const registerSchema = Joi.object({
  email: Joi.string().email().required(),
  password: Joi.string().min(8).required(),
  firstName: Joi.string().min(1).max(100).required(),
  lastName: Joi.string().min(1).max(100).required(),
  phone: Joi.string().pattern(/^\+?1?[2-9]\d{2}[2-9]\d{2}\d{4}$/).optional(),
  province: Joi.string().length(2).optional()
});

const loginSchema = Joi.object({
  email: Joi.string().email().required(),
  password: Joi.string().required()
});

// Register endpoint
router.post('/register', async (req, res) => {
  try {
    const { error, value } = registerSchema.validate(req.body);
    if (error) {
      return res.status(400).json({ error: error.details[0].message });
    }
    
    const { email, password, firstName, lastName, phone, province } = value;
    
    // Check if user already exists
    const existingUser = await pool.query(
      'SELECT id FROM users WHERE email = $1',
      [email]
    );
    
    if (existingUser.rows.length > 0) {
      return res.status(409).json({ error: 'User already exists' });
    }
    
    // Hash password
    const passwordHash = await bcrypt.hash(password, 12);
    
    // Create user
    const result = await pool.query(
      `INSERT INTO users (email, password_hash, first_name, last_name, phone, province) 
       VALUES ($1, $2, $3, $4, $5, $6) 
       RETURNING id, email, first_name, last_name, created_at`,
      [email, passwordHash, firstName, lastName, phone, province]
    );
    
    const user = result.rows[0];
    
    // Generate tokens
    const { accessToken, refreshToken } = JWTUtils.generateTokens({
      userId: user.id,
      email: user.email
    });
    
    // Store refresh token
    const refreshTokenHash = JWTUtils.hashRefreshToken(refreshToken);
    await pool.query(
      `INSERT INTO user_sessions (user_id, refresh_token_hash, device_info, ip_address, expires_at)
       VALUES ($1, $2, $3, $4, NOW() + INTERVAL '7 days')`,
      [user.id, refreshTokenHash, req.headers['user-agent'], req.ip]
    );
    
    // Log registration
    await pool.query(
      `INSERT INTO audit_logs (user_id, action, details, ip_address, user_agent)
       VALUES ($1, 'USER_REGISTERED', $2, $3, $4)`,
      [user.id, JSON.stringify({ email }), req.ip, req.headers['user-agent']]
    );
    
    res.status(201).json({
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name
      },
      accessToken,
      refreshToken
    });
    
  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Login endpoint
router.post('/login', async (req, res) => {
  try {
    const { error, value } = loginSchema.validate(req.body);
    if (error) {
      return res.status(400).json({ error: error.details[0].message });
    }
    
    const { email, password } = value;
    
    // Get user with login attempt tracking
    const result = await pool.query(
      `SELECT id, email, password_hash, first_name, last_name, login_attempts, 
              locked_until, is_verified, deleted_at
       FROM users WHERE email = $1`,
      [email]
    );
    
    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    
    const user = result.rows[0];
    
    // Check if account is deleted
    if (user.deleted_at) {
      return res.status(401).json({ error: 'Account not found' });
    }
    
    // Check if account is locked
    if (user.locked_until && new Date() < new Date(user.locked_until)) {
      return res.status(423).json({ error: 'Account temporarily locked' });
    }
    
    // Verify password
    const isValidPassword = await bcrypt.compare(password, user.password_hash);
    
    if (!isValidPassword) {
      // Increment login attempts
      const newAttempts = user.login_attempts + 1;
      const lockUntil = newAttempts >= 5 ? new Date(Date.now() + 15 * 60 * 1000) : null; // 15 min lock
      
      await pool.query(
        'UPDATE users SET login_attempts = $1, locked_until = $2 WHERE id = $3',
        [newAttempts, lockUntil, user.id]
      );
      
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    
    // Reset login attempts on successful login
    await pool.query(
      'UPDATE users SET login_attempts = 0, locked_until = NULL, last_login = NOW() WHERE id = $1',
      [user.id]
    );
    
    // Generate tokens
    const { accessToken, refreshToken } = JWTUtils.generateTokens({
      userId: user.id,
      email: user.email
    });
    
    // Store refresh token
    const refreshTokenHash = JWTUtils.hashRefreshToken(refreshToken);
    await pool.query(
      `INSERT INTO user_sessions (user_id, refresh_token_hash, device_info, ip_address, expires_at)
       VALUES ($1, $2, $3, $4, NOW() + INTERVAL '7 days')`,
      [user.id, refreshTokenHash, req.headers['user-agent'], req.ip]
    );
    
    // Log login
    await pool.query(
      `INSERT INTO audit_logs (user_id, action, ip_address, user_agent)
       VALUES ($1, 'USER_LOGIN', $2, $3)`,
      [user.id, req.ip, req.headers['user-agent']]
    );
    
    res.json({
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name,
        isVerified: user.is_verified
      },
      accessToken,
      refreshToken
    });
    
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Refresh token endpoint
router.post('/refresh', async (req, res) => {
  try {
    const { refreshToken } = req.body;
    
    if (!refreshToken) {
      return res.status(401).json({ error: 'Refresh token required' });
    }
    
    const refreshTokenHash = JWTUtils.hashRefreshToken(refreshToken);
    
    // Find valid session
    const sessionResult = await pool.query(
      `SELECT us.user_id, u.email 
       FROM user_sessions us
       JOIN users u ON us.user_id = u.id
       WHERE us.refresh_token_hash = $1 
       AND us.expires_at > NOW()
       AND u.deleted_at IS NULL`,
      [refreshTokenHash]
    );
    
    if (sessionResult.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid or expired refresh token' });
    }
    
    const { user_id, email } = sessionResult.rows[0];
    
    // Generate new tokens
    const { accessToken, refreshToken: newRefreshToken } = JWTUtils.generateTokens({
      userId: user_id,
      email
    });
    
    // Update session with new refresh token
    const newRefreshTokenHash = JWTUtils.hashRefreshToken(newRefreshToken);
    await pool.query(
      `UPDATE user_sessions 
       SET refresh_token_hash = $1, expires_at = NOW() + INTERVAL '7 days'
       WHERE refresh_token_hash = $2`,
      [newRefreshTokenHash, refreshTokenHash]
    );
    
    res.json({
      accessToken,
      refreshToken: newRefreshToken
    });
    
  } catch (error) {
    console.error('Token refresh error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Logout endpoint
router.post('/logout', authenticateToken, async (req, res) => {
  try {
    const { refreshToken } = req.body;
    
    if (refreshToken) {
      const refreshTokenHash = JWTUtils.hashRefreshToken(refreshToken);
      await pool.query(
        'DELETE FROM user_sessions WHERE refresh_token_hash = $1',
        [refreshTokenHash]
      );
    }
    
    // Log logout
    await pool.query(
      `INSERT INTO audit_logs (user_id, action, ip_address, user_agent)
       VALUES ($1, 'USER_LOGOUT', $2, $3)`,
      [req.user.id, req.ip, req.headers['user-agent']]
    );
    
    res.json({ message: 'Logged out successfully' });
    
  } catch (error) {
    console.error('Logout error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
```

## Step 5: Main Server

### server.js
```javascript
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { Pool } = require('pg');
require('dotenv').config();

const app = express();
const port = process.env.PORT || 3000;

// Database connection
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

// Initialize database
async function initDatabase() {
  try {
    const fs = require('fs');
    const path = require('path');
    const initSQL = fs.readFileSync(path.join(__dirname, 'database', 'init.sql'), 'utf8');
    await pool.query(initSQL);
    console.log('Database initialized successfully');
  } catch (error) {
    console.error('Database initialization error:', error);
  }
}

// Security middleware
app.use(helmet());
app.use(cors({
  origin: process.env.NODE_ENV === 'production' 
    ? ['https://northapp.ca', 'https://www.northapp.ca'] 
    : ['http://localhost:3000', 'http://localhost:8080'],
  credentials: true
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP'
});
app.use('/api/', limiter);

// Stricter rate limiting for auth endpoints
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10, // limit each IP to 10 auth requests per 15 minutes
  message: 'Too many authentication attempts'
});
app.use('/api/auth/', authLimiter);

// Body parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Trust proxy for Railway
app.set('trust proxy', 1);

// Routes
app.use('/api/auth', require('./routes/auth'));
app.use('/api/users', require('./routes/users'));
app.use('/api/accounts', require('./routes/accounts'));
app.use('/api/goals', require('./routes/goals'));

// Health check
app.get('/health', (req, res) => {
  res.json({ 
    status: 'OK', 
    timestamp: new Date().toISOString(),
    version: '1.0.0-alpha'
  });
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

// Error handler
app.use((error, req, res, next) => {
  console.error('Unhandled error:', error);
  res.status(500).json({ error: 'Internal server error' });
});

// Start server
app.listen(port, async () => {
  console.log(`North API running on port ${port}`);
  await initDatabase();
});
```

## Step 6: Deploy to Railway

### Environment Variables
```bash
# Set in Railway dashboard
railway variables set JWT_SECRET=your-super-long-random-secret-key-here
railway variables set NODE_ENV=production
railway variables set PLAID_CLIENT_ID=your-plaid-client-id
railway variables set PLAID_SECRET=your-plaid-secret
railway variables set PLAID_ENV=sandbox
```

### Deploy
```bash
# Initialize git if not already
git init
git add .
git commit -m "Initial North backend with custom auth"

# Connect to Railway and deploy
railway link
railway up

# Your API will be available at:
# https://your-project.railway.app
```

## Step 7: Test Your API

### Test Registration
```bash
curl -X POST https://your-project.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "province": "ON"
  }'
```

### Test Login
```bash
curl -X POST https://your-project.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

## Benefits of This Approach

✅ **PIPEDA Compliant** - All data in North America
✅ **Secure** - Proper password hashing, JWT tokens, rate limiting
✅ **Scalable** - Can handle thousands of users
✅ **Auditable** - Complete audit trail for compliance
✅ **Canadian-focused** - Province codes, postal codes, SIN handling
✅ **Mobile-ready** - JWT tokens work perfectly with mobile apps
✅ **Cost-effective** - $0 for alpha, scales with usage

## Next Steps
1. Add user profile endpoints
2. Add account management endpoints  
3. Add goal management endpoints
4. Integrate with Plaid for account linking
5. Add push notification support

Total setup time: ~1 hour
Cost: $0 for alpha testing