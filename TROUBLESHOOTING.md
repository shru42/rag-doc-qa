# RAG Document Q&A - Troubleshooting Guide

## Common Issues and Solutions

### Issue 1: Not Getting Responses from AI Model

#### Symptoms
- Query endpoint returns errors
- Empty or null responses
- Timeout errors

#### Root Causes & Solutions

**1. Missing or Invalid API Key**
```bash
# Check if environment variable is set
echo $GEMINI_API_KEY

# If not set, export it:
export GEMINI_API_KEY="your-actual-gemini-api-key"

# For permanent setup, add to ~/.bashrc or ~/.zshrc:
echo 'export GEMINI_API_KEY="your-api-key"' >> ~/.bashrc
source ~/.bashrc
```

**2. Database Not Running**
```bash
# Start PostgreSQL and Redis
docker-compose up -d

# Check if services are running
docker-compose ps

# Check logs
docker-compose logs postgres
docker-compose logs redis
```

**3. No Documents Uploaded**
```bash
# Upload a document first
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@your-document.pdf"
```

**4. Network/API Issues**
- Verify internet connection
- Check if Gemini API is accessible:
```bash
curl -H "Authorization: Bearer $GEMINI_API_KEY" \
  https://generativelanguage.googleapis.com/v1beta/models
```

### Issue 2: Application Won't Start

#### Check Prerequisites
```bash
# Java version (should be 17+)
java -version

# Maven version
mvn -version

# Docker running
docker ps
```

#### Start Services
```bash
# Start databases
docker-compose up -d

# Wait for services to be ready (30 seconds)
sleep 30

# Build and run application
mvn clean install
mvn spring-boot:run
```

### Issue 3: PDF Upload Fails

#### Common Causes
1. **File too large**: Max size is 50MB (configurable in application.yml)
2. **Corrupted PDF**: Try with a different PDF
3. **Password-protected PDF**: Remove password protection
4. **Database connection issue**: Check PostgreSQL is running

#### Test Upload
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@test.pdf" \
  -v
```

### Issue 4: Vector Store Issues

#### Check pgvector Extension
```bash
# Connect to database
docker exec -it rag-postgres psql -U postgres -d ragdb

# Check if pgvector is installed
\dx

# If not, install it:
CREATE EXTENSION IF NOT EXISTS vector;
```

### Issue 5: Redis Cache Issues

#### Clear Cache
```bash
# Connect to Redis
docker exec -it rag-redis redis-cli

# Clear all cache
FLUSHALL

# Check cache keys
KEYS *
```

## Debugging Steps

### 1. Check Application Health
```bash
curl http://localhost:8080/api/health
```

### 2. View Application Logs
```bash
# If running with Maven
mvn spring-boot:run

# Check for errors in the output
# Look for:
# - "AI model call failed"
# - "Failed to store chunks in vector database"
# - "Connection refused"
```

### 3. Test Each Component

**Test Database Connection:**
```bash
docker exec -it rag-postgres psql -U postgres -d ragdb -c "SELECT 1;"
```

**Test Redis Connection:**
```bash
docker exec -it rag-redis redis-cli PING
```

**Test API Key:**
```bash
curl -H "Authorization: Bearer $GEMINI_API_KEY" \
  https://generativelanguage.googleapis.com/v1beta/openai/models
```

### 4. Enable Debug Logging

Update `application.yml`:
```yaml
logging:
  level:
    com.shruti.ragdocqa: DEBUG
    org.springframework.ai: DEBUG
```

## Complete Setup Checklist

- [ ] Java 17+ installed
- [ ] Maven installed
- [ ] Docker and Docker Compose installed
- [ ] GEMINI_API_KEY environment variable set
- [ ] PostgreSQL running (port 5432)
- [ ] Redis running (port 6379)
- [ ] pgvector extension enabled
- [ ] Application built successfully
- [ ] At least one PDF document uploaded
- [ ] Health endpoint returns 200 OK

## Testing the Complete Flow

```bash
# 1. Start services
docker-compose up -d
sleep 30

# 2. Set API key
export GEMINI_API_KEY="your-key-here"

# 3. Start application
mvn spring-boot:run &

# Wait for startup
sleep 20

# 4. Check health
curl http://localhost:8080/api/health

# 5. Upload a document
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@sample.pdf"

# 6. Query the document
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is this document about?"}'
```

## Error Messages and Solutions

| Error Message | Solution |
|---------------|----------|
| "Failed to generate answer from AI model" | Check GEMINI_API_KEY is set and valid |
| "Failed to store document in vector database" | Ensure PostgreSQL is running and pgvector is installed |
| "I couldn't find relevant information" | Upload documents first |
| "Connection refused" | Start docker-compose services |
| "AI model returned an empty response" | Check API key and internet connection |
| "Failed to parse PDF" | Ensure PDF is not corrupted or password-protected |

## Getting Help

If issues persist:
1. Check application logs for detailed error messages
2. Verify all environment variables are set correctly
3. Ensure all services are running: `docker-compose ps`
4. Test each component individually
5. Check Gemini API status and quotas

## Performance Tips

1. **Increase chunk size** for longer documents (in application.yml)
2. **Adjust top-k** value for more/fewer context chunks
3. **Enable caching** to avoid repeated AI calls
4. **Monitor database** connection pool size
5. **Check Redis memory** usage for cache