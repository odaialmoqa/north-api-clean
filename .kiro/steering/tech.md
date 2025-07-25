# North Financial - Technology Stack

## Architecture

**Mobile App**: Kotlin Multiplatform with Jetpack Compose UI
**Backend**: Node.js with Express.js REST API
**Database**: PostgreSQL
**Deployment**: Railway (backend), Android/iOS app stores

## Mobile App Stack

- **Framework**: Kotlin Multiplatform Mobile (KMM)
- **UI**: Jetpack Compose (Android), SwiftUI integration (iOS)
- **HTTP Client**: Ktor
- **Serialization**: kotlinx.serialization
- **Dependency Injection**: Koin
- **Database**: SQLDelight with SQLCipher for encryption
- **Financial Integration**: Plaid Android/iOS SDKs
- **Build System**: Gradle with Kotlin DSL

### Key Libraries
- `androidx.compose.*` - UI framework
- `io.ktor:ktor-client-*` - HTTP networking
- `org.jetbrains.kotlinx:kotlinx-*` - Coroutines, serialization, datetime
- `com.plaid.link:sdk-core` - Plaid integration
- `io.insert-koin:koin-*` - Dependency injection

## Backend Stack

- **Runtime**: Node.js 14+
- **Framework**: Express.js
- **Database**: PostgreSQL with `pg` driver
- **Authentication**: JWT with bcryptjs
- **Security**: Helmet, CORS, rate limiting
- **Financial API**: Plaid Node.js SDK
- **Environment**: dotenv for configuration

## Common Commands

### Mobile App
```bash
# Build Android
cd mobile-app && ./gradlew assembleDebug

# Run tests (currently disabled)
cd mobile-app && ./gradlew test

# Clean build
cd mobile-app && ./gradlew clean
```

### Backend
```bash
# Development server
npm run dev

# Production server
npm start

# Test API connection
node test-api-connection.kt
```

### Deployment
```bash
# Deploy backend to Railway
./deploy-server.sh

# Deploy Android to device
./deploy-to-pixel.sh
```

## Configuration

- **Backend**: Environment variables via `.env` (development) or Railway (production)
- **Mobile**: `local.properties` for API keys and local config
- **Build**: `gradle/libs.versions.toml` for dependency version management