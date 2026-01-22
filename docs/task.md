# Frontend Development Task Breakdown

## Phase 1: Project Setup
- [x] Initialize Vue 3 + TypeScript project in `frontend/` directory
- [x] Install core dependencies (Element Plus, Axios, Pinia, Vue Router)
- [x] Configure Vite proxy for backend API

## Phase 2: Core Configuration
- [x] Set up Axios instance with interceptors
- [x] Configure Vue Router with basic routes
- [x] Set up Pinia stores (user, chat)
- [x] Configure Element Plus theming

## Phase 3: Authentication Module
- [x] Create Login page component
- [x] Implement login API service
- [x] Set up user store with token management
- [x] Add route guards for authentication

## Phase 4: Layout Structure
- [x] Create main Layout component (Header + Sidebar)
- [x] Implement navigation menu
- [x] Add user profile section
- [x] Set up responsive design

## Phase 5: Verification
- [x] Test login flow and token persistence
- [x] Verify API proxy configuration
- [x] Test responsive layout
## Phase 6: Debugging & Polish
- [x] Debug file upload failure - Fixed API endpoints (/doc/*)
- [x] Debug chat creation failure - Fixed API endpoints (/chat/send)
- [x] Beautify Login page - Glassmorphism + animated bubbles
- [x] Fix viewport CSS - Removed Vite default dark styles
- [x] Polish Chat interface - Updated layout
- [x] Add Stop Button & Timeout Handling
- [x] Switch to DeepSeek API (OpenRouter)
- [x] Add Context (Session Persistence)
- [x] Add Markdown Rendering
- [x] Add Chat History with Session List

## Phase 7: Document Management Module
- [x] Backend Implementation (DocumentController, DocumentService, MinioService)
- [x] Frontend DocList.vue with modern UI
- [x] File upload with progress
- [x] File list with icons and colors
- [x] Download with JWT authentication
- [x] Delete functionality
- [x] Full flow verification
- [ ] Final System Testing

## Phase 8: RAG & Knowledge Base Implementation
- [x] **Infrastructure**: Verify `pgvector` configuration in docker-compose
- [x] **Python AI Service**:
    - [x] Install dependencies (`langchain`, `psycopg2`, `sentence-transformers`)
    - [x] Implement PDF/Markdown text extraction
    - [x] Implement Text Chunking & Embedding logic
    - [x] Create API endpoint `/api/v1/ingest` (Document -> Vectors)
    - [x] Update `/api/v1/chat/stream` to support Context Retrieval
    - [x] Add timeout handling (connect: 10s, read: 120s)
- [x] **Java Backend**:
    - [x] Create `RagService` to call Python ingest API
    - [x] Trigger ingestion automatically when file is uploaded
- [ ] **Frontend**:
    - [ ] Add "Indexing Status" column in Document List
    - [ ] Add "Search/RAG" toggle in Chat Interface (Optional)
