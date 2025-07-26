# North App Server Deployment Guide

## 🚀 Quick Deployment

Your North app server is ready to deploy with enhanced AI CFO functionality!

### Server Features
- ✅ User authentication (register/login)
- ✅ AI CFO chat with onboarding
- ✅ Financial data endpoints
- ✅ Goal management
- ✅ Transaction tracking
- ✅ Enhanced security & rate limiting

## Environment Variables Required

Create a `.env` file or set these environment variables:

```bash
# Database
DATABASE_URL=postgresql://username:password@host:port/database

# Security
JWT_SECRET=your-super-secure-random-string-here

# Server
PORT=3000
NODE_ENV=production

# Email (Optional - for password reset)
RESEND_API_KEY=your-resend-api-key
```

## Railway Deployment (Recommended)

1. **Connect to Railway:**
   ```bash
   # Install Railway CLI
   npm install -g @railway/cli
   
   # Login to Railway
   railway login
   
   # Deploy from north-backend-only directory
   cd north-backend-only
   railway up
   ```

2. **Set Environment Variables in Railway:**
   - Go to your Railway project dashboard
   - Add the environment variables listed above
   - Railway will automatically provide a PostgreSQL database

3. **Custom Domain (Optional):**
   - In Railway dashboard, go to Settings > Domains
   - Add your custom domain or use the provided Railway domain

## Alternative Deployment Options

### Heroku
```bash
cd north-backend-only
heroku create your-north-app
heroku addons:create heroku-postgresql:mini
heroku config:set JWT_SECRET=your-secret-here
git push heroku main
```

### DigitalOcean App Platform
1. Connect your GitHub repository
2. Select the `north-backend-only` folder as the source
3. Add environment variables in the dashboard
4. Deploy

### Docker Deployment
```bash
cd north-backend-only

# Create Dockerfile
cat > Dockerfile << EOF
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
EXPOSE 3000
CMD ["npm", "start"]
EOF

# Build and run
docker build -t north-app .
docker run -p 3000:3000 --env-file .env north-app
```

## Database Setup

The server automatically creates the required tables on startup. No manual database setup needed!

Tables created:
- `users` - User accounts and authentication

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/forgot-password` - Password reset

### AI CFO
- `POST /api/ai/chat` - Enhanced AI CFO chat
- `POST /api/ai/onboarding/start` - Start AI CFO onboarding

### Financial Data
- `GET /api/financial/summary` - User's financial overview
- `GET /api/goals` - User's financial goals
- `GET /api/transactions` - User's transactions

### Health & Debug
- `GET /health` - Server health check
- `GET /debug` - Environment debug info

## Testing Your Deployment

1. **Health Check:**
   ```bash
   curl https://your-domain.com/health
   ```

2. **API Test:**
   ```bash
   curl https://your-domain.com/api
   ```

3. **Register Test User:**
   ```bash
   curl -X POST https://your-domain.com/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "password": "testpassword123",
       "firstName": "Test",
       "lastName": "User"
     }'
   ```

## Mobile App Configuration

Update your mobile app's API base URL to point to your deployed server:

```kotlin
// In your API configuration
const val BASE_URL = "https://your-deployed-server.com/api/"
```

## Security Notes

- ✅ HTTPS enforced in production
- ✅ Rate limiting enabled
- ✅ CORS configured
- ✅ Helmet security headers
- ✅ Password hashing with bcrypt
- ✅ JWT token authentication

## Monitoring

Monitor your deployment:
- Check `/health` endpoint regularly
- Monitor database connections
- Watch for rate limit violations
- Monitor memory and CPU usage

## Support

If you encounter issues:
1. Check the server logs
2. Verify environment variables are set
3. Test database connectivity
4. Check the `/debug` endpoint for configuration issues

Your North app is ready to provide an amazing AI CFO experience! 🎉