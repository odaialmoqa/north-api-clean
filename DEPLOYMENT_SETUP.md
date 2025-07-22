# North Backend - $0 Deployment Guide

## 🚀 AWS Free Tier Deployment (Canada Central)

### Free Tier Limits (12 months)
- **EC2**: 750 hours/month of t2.micro instances
- **RDS**: 750 hours/month of db.t2.micro + 20GB storage
- **Lambda**: 1M requests/month + 400,000 GB-seconds
- **API Gateway**: 1M API calls/month
- **S3**: 5GB storage + 20,000 GET requests
- **CloudWatch**: 10 custom metrics + 5GB log ingestion

### Cost Estimate for Alpha (0-1000 users)
```
Monthly costs after free tier:
- RDS PostgreSQL (db.t3.micro): ~$15/month
- EC2 (if needed): $0 (within free tier)
- Data transfer: ~$1-5/month
- Total: ~$16-20/month maximum
```

## Architecture for Alpha Release

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Mobile App    │───▶│   API Gateway    │───▶│   Lambda APIs   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                       ┌──────────────────┐             │
                       │   RDS PostgreSQL │◀────────────┘
                       │  (Canada Central)│
                       └──────────────────┘
```

## Step 1: AWS Account Setup

### Create AWS Account
1. Go to https://aws.amazon.com
2. Create account (requires credit card, but won't charge for free tier)
3. Verify phone number and email
4. Choose "Basic Support Plan" (free)

### Set Up Billing Alerts
```bash
# Set up billing alert to avoid surprises
aws budgets create-budget \
  --account-id YOUR_ACCOUNT_ID \
  --budget '{
    "BudgetName": "North-Backend-Budget",
    "BudgetLimit": {
      "Amount": "25",
      "Unit": "USD"
    },
    "TimeUnit": "MONTHLY",
    "BudgetType": "COST"
  }'
```

## Step 2: Infrastructure Setup

### Install AWS CLI
```bash
# macOS
brew install awscli

# Configure with your credentials
aws configure
# AWS Access Key ID: [Your Key]
# AWS Secret Access Key: [Your Secret]
# Default region name: ca-central-1
# Default output format: json
```

### Create VPC and Security Groups
```bash
# Create VPC
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --region ca-central-1 \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=north-vpc}]'

# Create subnets
aws ec2 create-subnet \
  --vpc-id vpc-xxxxxxxxx \
  --cidr-block 10.0.1.0/24 \
  --availability-zone ca-central-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=north-subnet-1}]'
```

## Step 3: Database Setup (RDS Free Tier)

### Create PostgreSQL Database
```bash
# Create DB subnet group first
aws rds create-db-subnet-group \
  --db-subnet-group-name north-db-subnet-group \
  --db-subnet-group-description "North DB subnet group" \
  --subnet-ids subnet-xxxxxxxxx subnet-yyyyyyyyy \
  --region ca-central-1

# Create PostgreSQL instance (FREE TIER)
aws rds create-db-instance \
  --db-instance-identifier north-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 14.9 \
  --master-username northadmin \
  --master-user-password YourSecurePassword123! \
  --allocated-storage 20 \
  --storage-type gp2 \
  --vpc-security-group-ids sg-xxxxxxxxx \
  --db-subnet-group-name north-db-subnet-group \
  --backup-retention-period 7 \
  --region ca-central-1 \
  --no-multi-az \
  --no-publicly-accessible
```

### Database Schema
```sql
-- Connect to your RDS instance and run:
CREATE DATABASE north_production;

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_verified BOOLEAN DEFAULT FALSE,
    last_login TIMESTAMP
);

-- Accounts table
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    plaid_account_id VARCHAR(255),
    account_name VARCHAR(255),
    account_type VARCHAR(50),
    balance DECIMAL(12,2),
    currency VARCHAR(3) DEFAULT 'CAD',
    institution_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID REFERENCES accounts(id) ON DELETE CASCADE,
    plaid_transaction_id VARCHAR(255),
    amount DECIMAL(12,2),
    description TEXT,
    category VARCHAR(100),
    date DATE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Goals table
CREATE TABLE financial_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255),
    target_amount DECIMAL(12,2),
    current_amount DECIMAL(12,2) DEFAULT 0,
    target_date DATE,
    priority INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_goals_user_id ON financial_goals(user_id);
```

## Step 4: Lambda Functions (Serverless APIs)

### Authentication Service
```javascript
// auth-service/index.js
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
});

exports.handler = async (event) => {
  const { httpMethod, path, body } = event;
  
  try {
    switch (`${httpMethod} ${path}`) {
      case 'POST /auth/register':
        return await register(JSON.parse(body));
      case 'POST /auth/login':
        return await login(JSON.parse(body));
      case 'POST /auth/refresh':
        return await refreshToken(JSON.parse(body));
      default:
        return {
          statusCode: 404,
          body: JSON.stringify({ error: 'Not found' })
        };
    }
  } catch (error) {
    return {
      statusCode: 500,
      body: JSON.stringify({ error: error.message })
    };
  }
};

async function register({ email, password }) {
  const hashedPassword = await bcrypt.hash(password, 10);
  
  const result = await pool.query(
    'INSERT INTO users (email, password_hash) VALUES ($1, $2) RETURNING id, email',
    [email, hashedPassword]
  );
  
  const user = result.rows[0];
  const token = jwt.sign({ userId: user.id }, process.env.JWT_SECRET, { expiresIn: '24h' });
  
  return {
    statusCode: 201,
    body: JSON.stringify({ user, token })
  };
}

async function login({ email, password }) {
  const result = await pool.query(
    'SELECT id, email, password_hash FROM users WHERE email = $1',
    [email]
  );
  
  if (result.rows.length === 0) {
    return {
      statusCode: 401,
      body: JSON.stringify({ error: 'Invalid credentials' })
    };
  }
  
  const user = result.rows[0];
  const isValid = await bcrypt.compare(password, user.password_hash);
  
  if (!isValid) {
    return {
      statusCode: 401,
      body: JSON.stringify({ error: 'Invalid credentials' })
    };
  }
  
  const token = jwt.sign({ userId: user.id }, process.env.JWT_SECRET, { expiresIn: '24h' });
  
  return {
    statusCode: 200,
    body: JSON.stringify({ 
      user: { id: user.id, email: user.email }, 
      token 
    })
  };
}
```

### User Service
```javascript
// user-service/index.js
const { Pool } = require('pg');
const jwt = require('jsonwebtoken');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
});

exports.handler = async (event) => {
  const { httpMethod, path, headers, body } = event;
  
  // Verify JWT token
  const token = headers.Authorization?.replace('Bearer ', '');
  if (!token) {
    return {
      statusCode: 401,
      body: JSON.stringify({ error: 'No token provided' })
    };
  }
  
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const userId = decoded.userId;
    
    switch (`${httpMethod} ${path}`) {
      case 'GET /user/profile':
        return await getUserProfile(userId);
      case 'PUT /user/profile':
        return await updateUserProfile(userId, JSON.parse(body));
      case 'GET /user/accounts':
        return await getUserAccounts(userId);
      case 'GET /user/goals':
        return await getUserGoals(userId);
      case 'POST /user/goals':
        return await createGoal(userId, JSON.parse(body));
      default:
        return {
          statusCode: 404,
          body: JSON.stringify({ error: 'Not found' })
        };
    }
  } catch (error) {
    return {
      statusCode: 500,
      body: JSON.stringify({ error: error.message })
    };
  }
};

async function getUserProfile(userId) {
  const result = await pool.query(
    'SELECT id, email, created_at FROM users WHERE id = $1',
    [userId]
  );
  
  return {
    statusCode: 200,
    body: JSON.stringify({ user: result.rows[0] })
  };
}

async function getUserAccounts(userId) {
  const result = await pool.query(
    'SELECT * FROM accounts WHERE user_id = $1 ORDER BY created_at DESC',
    [userId]
  );
  
  return {
    statusCode: 200,
    body: JSON.stringify({ accounts: result.rows })
  };
}

async function getUserGoals(userId) {
  const result = await pool.query(
    'SELECT * FROM financial_goals WHERE user_id = $1 ORDER BY created_at DESC',
    [userId]
  );
  
  return {
    statusCode: 200,
    body: JSON.stringify({ goals: result.rows })
  };
}

async function createGoal(userId, { title, target_amount, target_date, priority }) {
  const result = await pool.query(
    'INSERT INTO financial_goals (user_id, title, target_amount, target_date, priority) VALUES ($1, $2, $3, $4, $5) RETURNING *',
    [userId, title, target_amount, target_date, priority || 1]
  );
  
  return {
    statusCode: 201,
    body: JSON.stringify({ goal: result.rows[0] })
  };
}
```

## Step 5: Deploy Lambda Functions

### Package and Deploy
```bash
# Create deployment package for auth service
cd auth-service
npm init -y
npm install jsonwebtoken bcryptjs pg
zip -r auth-service.zip .

# Deploy to Lambda
aws lambda create-function \
  --function-name north-auth-service \
  --runtime nodejs18.x \
  --role arn:aws:iam::YOUR_ACCOUNT:role/lambda-execution-role \
  --handler index.handler \
  --zip-file fileb://auth-service.zip \
  --region ca-central-1 \
  --environment Variables='{
    "DATABASE_URL":"postgresql://northadmin:password@your-rds-endpoint:5432/north_production",
    "JWT_SECRET":"your-super-secret-jwt-key"
  }'

# Repeat for user service
cd ../user-service
npm init -y
npm install jsonwebtoken pg
zip -r user-service.zip .

aws lambda create-function \
  --function-name north-user-service \
  --runtime nodejs18.x \
  --role arn:aws:iam::YOUR_ACCOUNT:role/lambda-execution-role \
  --handler index.handler \
  --zip-file fileb://user-service.zip \
  --region ca-central-1 \
  --environment Variables='{
    "DATABASE_URL":"postgresql://northadmin:password@your-rds-endpoint:5432/north_production",
    "JWT_SECRET":"your-super-secret-jwt-key"
  }'
```

## Step 6: API Gateway Setup

### Create REST API
```bash
# Create API Gateway
aws apigateway create-rest-api \
  --name north-api \
  --description "North Financial App API" \
  --region ca-central-1

# Get the API ID from the response, then create resources and methods
# This creates endpoints like:
# POST /auth/register
# POST /auth/login  
# GET /user/profile
# GET /user/accounts
# POST /user/goals
```

## Environment Variables Setup
```bash
# Create .env file for local development
DATABASE_URL=postgresql://northadmin:password@your-rds-endpoint.ca-central-1.rds.amazonaws.com:5432/north_production
JWT_SECRET=your-super-secret-jwt-key-make-it-long-and-random
PLAID_CLIENT_ID=your_plaid_client_id
PLAID_SECRET=your_plaid_secret
PLAID_ENV=sandbox
AWS_REGION=ca-central-1
```

## Testing Your API
```bash
# Test registration
curl -X POST https://your-api-id.execute-api.ca-central-1.amazonaws.com/prod/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Test login
curl -X POST https://your-api-id.execute-api.ca-central-1.amazonaws.com/prod/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Test user profile (use token from login response)
curl -X GET https://your-api-id.execute-api.ca-central-1.amazonaws.com/prod/user/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Cost Monitoring
```bash
# Check your current usage
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --region ca-central-1
```

## Next Steps
1. Set up Plaid integration for account linking
2. Add transaction sync functionality  
3. Implement basic analytics endpoints
4. Set up monitoring and logging
5. Configure CORS for mobile app access

Total setup time: ~2-3 hours
Monthly cost: $0-20 for alpha testing (mostly free tier)