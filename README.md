# North Financial

A personal finance management app that combines financial tracking with gamification and AI-powered guidance. Built with Kotlin Multiplatform for mobile and Node.js for the backend.

## 🚀 Quick Start

### Prerequisites
- Node.js 18+
- Android Studio (for mobile development)
- Xcode (for iOS development, macOS only)
- PostgreSQL database

### Backend Setup
1. Install dependencies:
   ```bash
   npm install
   ```

2. Configure environment variables:
   ```bash
   cp .env.example .env
   # Edit .env with your actual values
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

### Mobile App Setup
1. Navigate to mobile app directory:
   ```bash
   cd mobile-app
   ```

2. Build the project:
   ```bash
   ./gradlew build
   ```

3. Run on Android:
   ```bash
   ./gradlew assembleDebug
   ```

## 📱 Features

- **Personal CFO AI**: Friendly AI assistant for financial advice
- **Account Linking**: Secure bank account connections via Plaid
- **Gamification**: Points, levels, streaks, and achievements
- **Goal Management**: Financial goal setting and tracking
- **Spending Insights**: Transaction categorization and analysis
- **Privacy-First**: PIPEDA compliance with strong data protection

## 🏗️ Architecture

- **Mobile**: Kotlin Multiplatform with Jetpack Compose
- **Backend**: Node.js with Express.js REST API
- **Database**: PostgreSQL
- **AI**: Google Gemini for CFO features
- **Financial Data**: Plaid integration

## 📚 Documentation

Detailed documentation is available in the `docs/` directory:
- Implementation guides
- Deployment instructions
- API documentation
- Architecture decisions

## 🚀 Deployment

### Backend (Railway)
```bash
./deploy-server.sh
```

### Mobile App
```bash
./deploy-app.sh
```

## 🔒 Security

- Environment variables for all secrets
- JWT authentication
- Rate limiting
- CORS protection
- Input validation

## 📄 License

MIT License - see LICENSE file for details.