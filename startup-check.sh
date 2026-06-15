#!/bin/bash

echo "=========================================="
echo "RAG Document Q&A - Startup Check"
echo "=========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check functions
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1 is installed"
        return 0
    else
        echo -e "${RED}✗${NC} $1 is NOT installed"
        return 1
    fi
}

check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        echo -e "${GREEN}✓${NC} Port $1 is in use (service running)"
        return 0
    else
        echo -e "${RED}✗${NC} Port $1 is NOT in use (service not running)"
        return 1
    fi
}

check_env_var() {
    if [ -z "${!1}" ]; then
        echo -e "${RED}✗${NC} Environment variable $1 is NOT set"
        return 1
    else
        echo -e "${GREEN}✓${NC} Environment variable $1 is set"
        return 0
    fi
}

# Start checks
echo "1. Checking Prerequisites..."
echo "----------------------------"
check_command java
check_command mvn
check_command docker
check_command docker-compose
echo ""

echo "2. Checking Environment Variables..."
echo "------------------------------------"
check_env_var GEMINI_API_KEY
echo ""

echo "3. Checking Docker Services..."
echo "------------------------------"
if docker ps | grep -q rag-postgres; then
    echo -e "${GREEN}✓${NC} PostgreSQL container is running"
else
    echo -e "${RED}✗${NC} PostgreSQL container is NOT running"
    echo -e "${YELLOW}  Run: docker-compose up -d${NC}"
fi

if docker ps | grep -q rag-redis; then
    echo -e "${GREEN}✓${NC} Redis container is running"
else
    echo -e "${RED}✗${NC} Redis container is NOT running"
    echo -e "${YELLOW}  Run: docker-compose up -d${NC}"
fi
echo ""

echo "4. Checking Ports..."
echo "--------------------"
check_port 5432  # PostgreSQL
check_port 6379  # Redis
check_port 8080  # Application
echo ""

echo "5. Testing Database Connection..."
echo "---------------------------------"
if docker exec rag-postgres psql -U postgres -d ragdb -c "SELECT 1;" &> /dev/null; then
    echo -e "${GREEN}✓${NC} PostgreSQL connection successful"
else
    echo -e "${RED}✗${NC} PostgreSQL connection failed"
fi

if docker exec rag-redis redis-cli PING &> /dev/null; then
    echo -e "${GREEN}✓${NC} Redis connection successful"
else
    echo -e "${RED}✗${NC} Redis connection failed"
fi
echo ""

echo "6. Checking pgvector Extension..."
echo "----------------------------------"
if docker exec rag-postgres psql -U postgres -d ragdb -c "\dx" | grep -q vector; then
    echo -e "${GREEN}✓${NC} pgvector extension is installed"
else
    echo -e "${YELLOW}⚠${NC} pgvector extension might not be installed"
    echo -e "${YELLOW}  Run: docker exec rag-postgres psql -U postgres -d ragdb -c 'CREATE EXTENSION IF NOT EXISTS vector;'${NC}"
fi
echo ""

echo "=========================================="
echo "Summary"
echo "=========================================="

# Count issues
issues=0

if ! command -v java &> /dev/null; then ((issues++)); fi
if ! command -v mvn &> /dev/null; then ((issues++)); fi
if ! command -v docker &> /dev/null; then ((issues++)); fi
if [ -z "$GEMINI_API_KEY" ]; then ((issues++)); fi
if ! docker ps | grep -q rag-postgres; then ((issues++)); fi
if ! docker ps | grep -q rag-redis; then ((issues++)); fi

if [ $issues -eq 0 ]; then
    echo -e "${GREEN}All checks passed! You're ready to start the application.${NC}"
    echo ""
    echo "To start the application, run:"
    echo "  mvn spring-boot:run"
else
    echo -e "${RED}Found $issues issue(s). Please fix them before starting.${NC}"
    echo ""
    echo "Common fixes:"
    echo "  1. Set API key: export GEMINI_API_KEY='your-key'"
    echo "  2. Start services: docker-compose up -d"
    echo "  3. Wait 30 seconds for services to initialize"
fi

echo ""
echo "For detailed troubleshooting, see TROUBLESHOOTING.md"
echo "=========================================="

# Made with Bob
