const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const Joi = require('joi');
const crypto = require('crypto');

const router = express.Router();

// JWT configuration
const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-key-change-this-in-production';
const JWT_EXPIRES_IN = '15m';
const REFRESH_TOKEN_EXPIRES_IN = '7d';

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

// Utility functions
function generateTokens(payload) {
  const accessToken = jwt.sign(payload, JWT_SECRET, { 
    expiresIn: JWT_EXPIRES_IN 
  });
  
  const refreshToken = crypto.randomBytes(40).toString('hex');
  
  return { accessToken, refreshToken };
}

function hashRefreshToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

// Register endpoint
router.post('/register', async (req, res) => {
  const pool = req.app.locals.db;
  
  try {
    const { error, value } = registerSchema.validate(req.body);
    if (error) {
      return res.status(400).json({ 
        error: 'Validation failed',
        details: error.details[0].message 
      });
    }
    
    const { email, password, firstName, lastName, phone, province } = value;
    
    // Check if user already exists
    const existingUser = await pool.query(
      'SELECT id FROM users WHERE email = $1 AND deleted_at IS NULL',
      [email]
    );
    
    if (existingUser.rows.length > 0) {
      return res.status(409).json({ error: 'User already exists with this email' });
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
    const { accessToken, refreshToken } = generateTokens({
      userId: user.id,
      email: user.email
    });
    
    // Store refresh token
    const refreshTokenHash = hashRefreshToken(refreshToken);
    await pool.query(
      `INSERT INTO user_sessions (user_id, refresh_token_hash, device_info, ip_address, expires_at)
       VALUES ($1, $2, $3, $4, NOW() + INTERVAL '7 days')`,
      [user.id, refreshTokenHash, req.headers['user-agent'] || 'Unknown', req.ip]
    );
    
    console.log(`✅ New user registered: ${email}`);
    
    res.status(201).json({
      message: 'Registration successful',
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name,
        createdAt: user.created_at
      },
      accessToken,
      refreshToken
    });
    
  } catch (error) {
    console.error('Registration error:', error);
    
    // Handle specific database errors
    if (error.code === '23505') { // Unique violation
      return res.status(409).json({ error: 'User already exists with this email' });
    }
    
    res.status(500).json({ error: 'Registration failed. Please try again.' });
  }
});

// Login endpoint
router.post('/login', async (req, res) => {
  const pool = req.app.locals.db;
  
  try {
    const { error, value } = loginSchema.validate(req.body);
    if (error) {
      return res.status(400).json({ 
        error: 'Validation failed',
        details: error.details[0].message 
      });
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
      return res.status(401).json({ error: 'Invalid email or password' });
    }
    
    const user = result.rows[0];
    
    // Check if account is deleted
    if (user.deleted_at) {
      return res.status(401).json({ error: 'Account not found' });
    }
    
    // Check if account is locked
    if (user.locked_until && new Date() < new Date(user.locked_until)) {
      const lockTime = Math.ceil((new Date(user.locked_until) - new Date()) / 60000);
      return res.status(423).json({ 
        error: `Account temporarily locked. Try again in ${lockTime} minutes.` 
      });
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
      
      if (lockUntil) {
        return res.status(423).json({ 
          error: 'Too many failed attempts. Account locked for 15 minutes.' 
        });
      }
      
      return res.status(401).json({ 
        error: 'Invalid email or password',
        attemptsRemaining: 5 - newAttempts
      });
    }
    
    // Reset login attempts on successful login
    await pool.query(
      'UPDATE users SET login_attempts = 0, locked_until = NULL, last_login = NOW() WHERE id = $1',
      [user.id]
    );
    
    // Generate tokens
    const { accessToken, refreshToken } = generateTokens({
      userId: user.id,
      email: user.email
    });
    
    // Store refresh token
    const refreshTokenHash = hashRefreshToken(refreshToken);
    await pool.query(
      `INSERT INTO user_sessions (user_id, refresh_token_hash, device_info, ip_address, expires_at)
       VALUES ($1, $2, $3, $4, NOW() + INTERVAL '7 days')`,
      [user.id, refreshTokenHash, req.headers['user-agent'] || 'Unknown', req.ip]
    );
    
    console.log(`✅ User logged in: ${email}`);
    
    res.json({
      message: 'Login successful',
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
    res.status(500).json({ error: 'Login failed. Please try again.' });
  }
});

// Refresh token endpoint
router.post('/refresh', async (req, res) => {
  const pool = req.app.locals.db;
  
  try {
    const { refreshToken } = req.body;
    
    if (!refreshToken) {
      return res.status(401).json({ error: 'Refresh token required' });
    }
    
    const refreshTokenHash = hashRefreshToken(refreshToken);
    
    // Find valid session
    const sessionResult = await pool.query(
      `SELECT us.user_id, u.email, u.first_name, u.last_name
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
    
    const { user_id, email, first_name, last_name } = sessionResult.rows[0];
    
    // Generate new tokens
    const { accessToken, refreshToken: newRefreshToken } = generateTokens({
      userId: user_id,
      email
    });
    
    // Update session with new refresh token
    const newRefreshTokenHash = hashRefreshToken(newRefreshToken);
    await pool.query(
      `UPDATE user_sessions 
       SET refresh_token_hash = $1, expires_at = NOW() + INTERVAL '7 days'
       WHERE refresh_token_hash = $2`,
      [newRefreshTokenHash, refreshTokenHash]
    );
    
    res.json({
      message: 'Token refreshed successfully',
      user: {
        id: user_id,
        email,
        firstName: first_name,
        lastName: last_name
      },
      accessToken,
      refreshToken: newRefreshToken
    });
    
  } catch (error) {
    console.error('Token refresh error:', error);
    res.status(500).json({ error: 'Token refresh failed. Please login again.' });
  }
});

// Logout endpoint
router.post('/logout', async (req, res) => {
  const pool = req.app.locals.db;
  
  try {
    const { refreshToken } = req.body;
    
    if (refreshToken) {
      const refreshTokenHash = hashRefreshToken(refreshToken);
      await pool.query(
        'DELETE FROM user_sessions WHERE refresh_token_hash = $1',
        [refreshTokenHash]
      );
    }
    
    res.json({ message: 'Logged out successfully' });
    
  } catch (error) {
    console.error('Logout error:', error);
    res.status(500).json({ error: 'Logout failed' });
  }
});

// Test endpoint
router.get('/test', (req, res) => {
  res.json({
    message: 'North Auth API is working!',
    timestamp: new Date().toISOString(),
    endpoints: {
      register: 'POST /api/auth/register',
      login: 'POST /api/auth/login',
      refresh: 'POST /api/auth/refresh',
      logout: 'POST /api/auth/logout'
    }
  });
});

module.exports = router;