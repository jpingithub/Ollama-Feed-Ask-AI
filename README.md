# Feed Ask AI - PDF Document Q&A System

A Spring Boot application that enables users to upload PDF documents, embed them using Ollama's embedding models, store embeddings in MySQL, and ask questions based on the document content using RAG (Retrieval Augmented Generation).

## Architecture

```
┌─────────────┐
│  Client     │
└──────┬──────┘
       │
       ├─── POST /api/v1/feed-ask/feed ──────┐
       │                                      │
       └─── GET /api/v1/feed-ask/ask?prompt ─┤
                                              │
       ┌──────────────────────────────────────┘
       │
┌──────▼──────────────────────────┐
│   Spring Boot Application       │
│  - PDF Processing Service      │
│  - Embedding Service           │
│  - Vector Search Service       │
│  - Ollama Integration          │
└───────┬────────────┬───────────┘
        │            │
        ▼            ▼
   ┌────────────┐  ┌──────────────────┐
   │  MySQL     │  │  Ollama Models   │
   │  (3307)    │  │  (11435)         │
   └────────────┘  │- nomic-embed-text│
                   │- tinyllama       │
                   └──────────────────┘
```

## Features

- **PDF Upload & Processing**: Upload PDF files and automatically extract text content
- **Smart Text Chunking**: Splits PDF content into manageable chunks with overlap
- **Embedding Generation**: Uses Ollama's `nomic-embed-text` model for semantic embeddings
- **Vector Storage**: Efficiently stores embeddings in MySQL database
- **Semantic Search**: Finds top 3 most similar vectors using cosine similarity
- **Intelligent Q&A**: Uses `tinyllama` model to generate context-aware answers
- **OpenAPI Documentation**: Full API documentation with Swagger UI
- **Error Handling**: Comprehensive exception handling with meaningful error messages
- **Performance Metrics**: Returns processing time for all operations

## Prerequisites

- Docker and Docker Compose
- Java 21
- Gradle

## Quick Start

### 1. Start Docker Containers

From the project root directory:

```bash
docker-compose up -d
```

This will start:
- **MySQL 8.0.36** on port 3307
- **Ollama** on port 11435

### 2. Pull Required Ollama Models

Once Ollama is running, pull the required models:

```bash
# Pull embedding model
docker exec feed-ask-ai-ollama ollama pull nomic-embed-text

# Pull answer model
docker exec feed-ask-ai-ollama ollama pull tinyllama
```

### 3. Build and Run the Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## API Endpoints

### 1. Upload PDF for Embedding

**Endpoint:** `POST /api/v1/feed-ask/feed`

**Content-Type:** `multipart/form-data`

**Request:**
```bash
curl -X POST \
  -F "file=@sample.pdf" \
  http://localhost:8080/api/v1/feed-ask/feed
```

**Response (200 OK):**
```json
{
  "documentId": 1,
  "fileName": "sample.pdf",
  "embeddingCount": 15,
  "processingTimeMs": 8450,
  "message": "PDF successfully uploaded and embedded with 15 embeddings"
}
```

### 2. Ask a Question

**Endpoint:** `GET /api/v1/feed-ask/ask`

**Query Parameters:**
- `prompt` (required, string): The question to ask

**Request:**
```bash
curl -X GET \
  "http://localhost:8080/api/v1/feed-ask/ask?prompt=What%20is%20the%20main%20topic%20of%20the%20document?"
```

**Response (200 OK):**
```json
{
  "answer": "Based on the document content, the main topic is about...",
  "sourceVectors": [
    {
      "sourceText": "This is a relevant excerpt from the PDF...",
      "similarityScore": 0.87,
      "pageNumber": 2,
      "fileName": "sample.pdf"
    },
    {
      "sourceText": "Another matching text snippet...",
      "similarityScore": 0.82,
      "pageNumber": 5,
      "fileName": "sample.pdf"
    },
    {
      "sourceText": "Additional context text...",
      "similarityScore": 0.78,
      "pageNumber": 8,
      "fileName": "sample.pdf"
    }
  ],
  "processingTimeMs": 6230
}
```

## Error Handling

The API returns appropriate HTTP status codes and error messages:

| Status Code | Scenario |
|------------|----------|
| 200 | Successful request |
| 400 | Invalid PDF file, empty prompt, or validation error |
| 413 | File size exceeds 100MB limit |
| 500 | Internal server error |
| 503 | Ollama service unavailable |

**Error Response Example:**
```json
{
  "timestamp": "2024-05-04T10:30:00",
  "status": 400,
  "error": "PDF Processing Error",
  "message": "File must be a PDF. Received: text/plain",
  "path": "/api/v1/feed-ask/feed"
}
```

## Configuration

### Database Configuration
File: `src/main/resources/application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/feed_ask_ai
    username: admin
    password: admin123
```

### Ollama Configuration
```yaml
ollama:
  base-url: http://localhost:11435
```

### File Upload Configuration
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

## Docker Compose Configuration

The `docker-compose.yml` file includes:

### MySQL Service
- **Image:** mysql:8.0.36
- **Port:** 3307
- **Database:** feed_ask_ai
- **Default User:** admin / admin123
- **Volume:** mysql_data (persistent)

### Ollama Service
- **Image:** ollama/ollama:latest
- **Port:** 11435
- **Volume:** ollama_data (persistent)

## Understanding the Processing Flow

### PDF Upload Flow

```
1. User uploads PDF file
   ↓
2. PdfProcessingService extracts text
   ↓
3. Text is chunked into manageable pieces (500 chars with 100 char overlap)
   ↓
4. PdfDocument entity is created and saved
   ↓
5. For each text chunk:
   - OllamaService generates embedding using nomic-embed-text
   - Embedding is converted to bytes and stored in database
   ↓
6. Response returned with embedding count and processing time
```

### Question Answering Flow

```
1. User asks a question
   ↓
2. OllamaService generates embedding for the question
   ↓
3. VectorSearchService finds top 3 most similar embeddings
   - Calculates cosine similarity for all stored embeddings
   - Returns top 3 matches with similarity scores
   ↓
4. VectorSearchService builds context from matched texts
   ↓
5. OllamaService generates answer using tinyllama model
   - Model receives question + context
   - Returns AI-generated answer
   ↓
6. Response returned with answer, source vectors, and metrics
```

## Vector Similarity Calculation

The system uses **Cosine Similarity** to find relevant documents:

```
Similarity = (A · B) / (||A|| × ||B||)

where:
- A and B are embedding vectors
- · is dot product
- ||A|| and ||B|| are vector magnitudes

Score normalized to range [0, 1]:
- 0 = completely different
- 1 = identical
```

## API Documentation

Access the interactive Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

Or view the OpenAPI JSON at:
```
http://localhost:8080/v3/api-docs
```

## Performance Considerations

1. **Embedding Generation**: Typically 2-5 seconds per chunk depending on model load
2. **Vector Search**: Millisecond-level performance for similarity matching
3. **Answer Generation**: 3-10 seconds depending on model response time
4. **Database Indexes**: Configured on `pdf_document_id` for faster lookups

## Troubleshooting

### Ollama Connection Issues
```bash
# Check if Ollama is running
curl http://localhost:11435/api/tags

# View Ollama logs
docker logs feed-ask-ai-ollama
```

### MySQL Connection Issues
```bash
# Check if MySQL is running
docker exec feed-ask-ai-mysql mysql -u admin -padmin123 -e "SELECT 1"

# View MySQL logs
docker logs feed-ask-ai-mysql
```

### Model Loading
Ensure models are pulled before making requests:
```bash
docker exec feed-ask-ai-ollama ollama list
```

## Dependencies

Key dependencies included in build.gradle:
- Spring Boot 4.0.6
- Spring AI 2.0.0-M5
- JPA/Hibernate
- MySQL Connector
- SpringDoc OpenAPI (Swagger)
- Lombok
- Apache Commons Lang

## Project Structure

```
src/
├── main/
│   ├── java/com/rb/feed_ask_ai/
│   │   ├── controller/
│   │   │   ├── FeedController.java
│   │   │   └── AskController.java
│   │   ├── service/
│   │   │   ├── FeedService.java
│   │   │   ├── AskService.java
│   │   │   ├── OllamaService.java
│   │   │   ├── PdfProcessingService.java
│   │   │   ├── VectorSearchService.java
│   │   │   └── VectorOperationService.java
│   │   ├── entity/
│   │   │   ├── PdfDocument.java
│   │   │   └── Embedding.java
│   │   ├── repository/
│   │   │   ├── PdfDocumentRepository.java
│   │   │   └── EmbeddingRepository.java
│   │   ├── dto/
│   │   │   ├── FeedUploadResponseDto.java
│   │   │   ├── AskResponseDto.java
│   │   │   └── VectorMatchDto.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── PdfProcessingException.java
│   │   │   ├── EmbeddingException.java
│   │   │   └── OllamaException.java
│   │   └── FeedAskAiApplication.java
│   └── resources/
│       └── application.yaml
└── test/...
```

## Next Steps / Enhancements

Potential improvements for future versions:
- [ ] Add batch PDF upload support
- [ ] Implement PDF deletion with cascade embedding cleanup
- [ ] Add conversation history and chat memory
- [ ] Implement RAG scoring and relevance filtering
- [ ] Add support for multiple embedding models
- [ ] Implement caching for frequently asked questions
- [ ] Add analytics and usage metrics
- [ ] Implement API rate limiting
- [ ] Add authentication and authorization

## License

This project is licensed under the MIT License.

## Support

For issues or questions, please refer to the troubleshooting section or check the application logs.

