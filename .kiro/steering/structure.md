# North Financial - Project Structure

## Repository Layout

```
/
├── mobile-app/                 # Kotlin Multiplatform mobile application
├── north-backend-only/         # Separate backend instance (legacy)
├── server.js                   # Main backend server
├── routes/                     # Backend API routes
├── deploy-*.sh                 # Deployment scripts
└── *.md                        # Documentation and implementation summaries
```

## Mobile App Structure

```
mobile-app/
├── composeApp/                 # Main application module
│   └── src/
│       ├── androidMain/        # Android-specific code
│       ├── commonMain/         # Shared UI code
│       └── iosMain/            # iOS-specific code
├── shared/                     # Shared business logic module
│   └── src/
│       ├── androidMain/        # Android platform implementations
│       ├── commonMain/         # Cross-platform code
│       └── iosMain/            # iOS platform implementations
├── iosApp/                     # iOS application wrapper
└── gradle/                     # Build configuration
```

## Package Organization

### Shared Module (`mobile-app/shared/src/commonMain/kotlin/com/north/mobile/`)

- `data/` - Data layer (repositories, services, API clients)
  - `ai/` - AI/CFO service implementations
  - `analytics/` - Financial analytics and categorization
  - `api/` - HTTP API clients and models
  - `auth/` - Authentication and session management
  - `gamification/` - Points, achievements, streaks
  - `plaid/` - Plaid integration services
  - `privacy/` - PIPEDA compliance and data management
- `domain/` - Business models and validation
- `di/` - Dependency injection modules

### UI Module (`mobile-app/composeApp/src/commonMain/kotlin/com/north/mobile/ui/`)

- `accounts/` - Account connection and management screens
- `auth/` - Authentication screens
- `chat/` - AI CFO chat interface
- `dashboard/` - Main dashboard and navigation
- `onboarding/` - User onboarding flow
- `profile/` - User profile and settings

## Naming Conventions

- **Files**: PascalCase for classes (`UserRepository.kt`)
- **Packages**: lowercase with dots (`com.north.mobile.data.auth`)
- **Composables**: PascalCase with "Screen" or "Component" suffix
- **Services**: Interface + Impl pattern (`AuthService` + `AuthServiceImpl`)
- **Models**: Descriptive names (`FinancialGoal`, `PlaidAccount`)

## File Patterns

- **Disabled Files**: `.disabled` suffix for temporarily unused code
- **Platform Files**: `.android.kt` / `.ios.kt` for platform-specific implementations
- **Test Files**: Mirror source structure in `*Test/` directories
- **SQL Files**: `.sq` extension in `sqldelight/` directories

## Configuration Files

- `gradle/libs.versions.toml` - Centralized dependency versions
- `local.properties` - Local development configuration
- `.env` - Backend environment variables (development)
- `railway.json` - Production deployment configuration