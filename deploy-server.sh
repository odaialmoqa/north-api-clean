#!/bin/bash

# North Server Deployment Script
echo "🚀 Deploying North Server with AI CFO Enhancement..."

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if Railway CLI is installed
if command -v railway &> /dev/null; then
    print_status "Railway CLI found. Deploying to Railway..."
    
    # Check if already connected to Railway project
    if [ -f ".railway/project.json" ]; then
        print_status "Existing Railway project found. Deploying..."
        railway up
    else
        print_status "Creating new Railway project..."
        railway login
        railway up
    fi
    
    print_success "Server deployed to Railway!"
    print_status "Don't forget to set your environment variables in the Railway dashboard:"
    echo "   - DATABASE_URL (Railway will provide this automatically)"
    echo "   - JWT_SECRET (generate a secure random string)"
    
else
    print_warning "Railway CLI not found. Installing..."
    npm install -g @railway/cli
    
    print_status "Railway CLI installed. Please run this script again or deploy manually:"
    echo ""
    echo "Manual deployment steps:"
    echo "1. railway login"
    echo "2. railway up"
    echo ""
fi

print_success "🎉 Server deployment process complete!"
echo ""
echo "Next steps:"
echo "1. Set environment variables in your hosting platform"
echo "2. Test your API endpoints"
echo "3. Update your mobile app's API base URL"
echo ""
echo "Your enhanced AI CFO server is ready! 🤖💰"