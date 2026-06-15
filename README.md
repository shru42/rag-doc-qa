# RAG Document Q&A

Ask questions about any PDF — answers grounded in your document, not hallucinated.

**Stack:** Spring Boot 3.5 · Spring AI 1.0 · Gemini API · pgvector · Redis · Docker

---

## Architecture

```
PDF Upload
    │
    ▼
PDFBox (extract text)
    │
    ▼
Chunker (500 chars, 50 overlap)
    │
    ▼
Spring AI → Gemini Embeddings (gemini-embedding-001, 3072-dim)
    │
    ▼
pgvector (store & index vectors)


Query
    │
    ├─► Redis cache hit? → return cached answer
    │
    ▼
Spring AI → Gemini Embeddings (embed question)
    │
    ▼
pgvector similarity search (cosine, top-5 chunks)
    │
    ▼
Gemini 3.5 Flash (question + context → grounded answer)
    │
    ▼
Redis (cache answer for 1 hour)
    │
    ▼
Response
```

---

## Setup

### Prerequisites
- Java 21+
- Maven
- Docker Desktop

### 1. Start infrastructure
```bash
docker-compose up -d
```

This starts:
- PostgreSQL with pgvector extension on port 5432
- Redis on port 6379

### 2. Create vector table
```bash
docker exec -it rag-postgres psql -U postgres -d ragdb -c "
  CREATE EXTENSION IF NOT EXISTS vector;
  CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(3072)
  );"
```

### 3. Set your Gemini API key
Get a free API key from [Google AI Studio](https://aistudio.google.com).

```bash
export GEMINI_API_KEY=your-api-key-here
```

### 4. Run the app
```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`

---

## API

### Upload a PDF
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@your-document.pdf"
```

Response:
```json
{
  "success": true,
  "message": "Document ingested successfully",
  "fileName": "your-document.pdf",
  "chunksStored": 12
}
```

### Ask a question
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the main conclusion of the document?"}'
```

Response:
```json
{
  "success": true,
  "answer": "Based on the document, the main conclusion is..."
}
```

### Health check
```bash
curl http://localhost:8080/api/health
```

---

## Key concepts demonstrated

| Concept | Implementation |
|---------|---------------|
| RAG pipeline | `DocumentService` + `QueryService` |
| Vector embeddings | Spring AI + Gemini `gemini-embedding-001` |
| Semantic search | `VectorStore.similaritySearch()` with cosine distance |
| Redis caching | `@Cacheable` on `QueryService.answer()` — skips LLM on repeat queries |
| PDF parsing | Apache PDFBox 3.x (`Loader.loadPDF()`) |
| Sliding-window chunking | 500 char chunks, 50 char overlap |
| Grounded prompting | Context-only prompt in `buildPrompt()` prevents hallucination |
| OpenAI-compatible endpoint | Gemini served via `generativelanguage.googleapis.com/v1beta/openai` |

