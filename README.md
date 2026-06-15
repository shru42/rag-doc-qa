# RAG Document Q&A

Ask questions about any PDF — answers grounded in your document, not hallucinated.

**Stack:** Spring Boot 3 · Spring AI · Gemini API · pgvector · Redis · Docker

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
Spring AI → Gemini Embeddings (text-embedding-004)
    │
    ▼
pgvector (store 768-dim vectors)


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
Gemini 1.5 Flash (question + context → answer)
    │
    ▼
Redis (cache answer)
    │
    ▼
Response
```

---

## Setup

### 1. Prerequisites
- Java 21
- Maven
- Docker Desktop

### 2. Start infrastructure
```bash
docker-compose up -d
```

### 3. Set environment variable
```bash
export GOOGLE_PROJECT_ID=your-gcp-project-id
```
> Get this from Google Cloud Console. Enable Vertex AI API and Gemini API.

### 4. Run the app
```bash
mvn spring-boot:run
```

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
  "message": "Document ingested successfully",
  "fileName": "your-document.pdf",
  "chunksStored": 42
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
  "answer": "Based on the document, the main conclusion is..."
}
```

---

## Key concepts demonstrated

| Concept | Where |
|---------|-------|
| RAG pipeline | `DocumentService` + `QueryService` |
| Vector embeddings | Spring AI auto-embeds via Gemini |
| Semantic search | `VectorStore.similaritySearch()` |
| Redis caching | `@Cacheable` on `QueryService.answer()` |
| PDF parsing | Apache PDFBox in `DocumentService` |
| Sliding-window chunking | `chunkText()` method |
| Grounded prompting | `buildPrompt()` in `QueryService` |

---

## Resume bullet point

> Built a RAG-based Document Q&A system using Spring Boot, Spring AI, and Gemini API — PDF ingestion pipeline with sliding-window chunking, pgvector semantic search, and Redis response caching. Deployed with Docker Compose.
