# Frontend Development Implementation Plan

## Goal

Initialize and set up the Vue 3 + TypeScript frontend for the LLM-Research-Assistant project. This includes project scaffolding, dependency installation, basic configuration, and creating the authentication and layout structure.

## User Review Required

> [!IMPORTANT]
> **Prerequisites**: Ensure Node.js (v18 or higher) is installed on your system.

> [!NOTE]
> **Port Configuration**: 
> - Frontend dev server: `5173` (Vite default)
> - Backend API: `8080` (Java Spring Boot)
> - AI Service: `8000` (Python FastAPI)
> - Vite will proxy `/api` requests to `http://localhost:8080`

## Proposed Changes

### Frontend Infrastructure

#### [NEW] `frontend/` Directory Structure
```
frontend/
├── public/
├── src/
│   ├── api/          # API service modules
│   ├── assets/       # Static assets
│   ├── components/   # Reusable components
│   ├── router/       # Vue Router configuration
│   ├── stores/       # Pinia stores
│   ├── views/        # Page components
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

---

### Core Configuration Files

#### [NEW] [vite.config.ts](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/vite.config.ts)
- Configure Vite dev server
- Set up API proxy: `/api` → `http://localhost:8080`
- Configure build options

#### [NEW] [tsconfig.json](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/tsconfig.json)
- TypeScript configuration for Vue 3
- Path aliases (@/ → src/)
- Strict type checking

#### [NEW] [package.json](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/package.json)
- Dependencies:
  - `vue` (^3.4.0)
  - `element-plus` (^2.5.0)
  - `axios` (^1.6.0)
  - `vue-router` (^4.2.0)
  - `pinia` (^2.1.0)
  - `sass` (^1.70.0)

---

### API Layer

#### [NEW] [src/api/request.ts](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/api/request.ts)
- Create Axios instance with base configuration
- Request interceptor: Inject JWT token from localStorage
- Response interceptor: Handle 401 errors and token expiration
- Error handling utilities

#### [NEW] [src/api/auth.ts](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/api/auth.ts)
- `login(username, password)`: User authentication
- `logout()`: Clear session
- `getUserInfo()`: Fetch current user details

#### [NEW] [src/api/chat.ts](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/api/chat.ts)
- `streamChat(message, sessionId)`: SSE-based streaming chat
- `getSessions()`: Fetch chat history
- `deleteSession(sessionId)`: Delete chat session

#### [NEW] [src/api/doc.ts](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/api/doc.ts)
- `uploadDocument(file)`: Upload document to MinIO
- `getDocuments()`: List user documents
- `downloadDocument(docId)`: Download document
- `deleteDocument(docId)`: Delete document

---

### State Management (Pinia)

#### [NEW] [src/stores/userStore.ts](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/stores/userStore.ts)
- State: `token`, `userInfo`, `isLoggedIn`
- Actions:
  - `login(credentials)`: Authenticate and store token
  - `logout()`: Clear state and redirect
  - `loadUserFromLocalStorage()`: Restore session

#### [NEW] [src/stores/chatStore.ts](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/stores/chatStore.ts)
- State: `currentSessionId`, `messages`, `sessionList`
- Actions:
  - `sendMessage(content)`: Add user message and stream AI response
  - `loadSessions()`: Fetch chat history
  - `createNewSession()`: Start new chat

---

### Routing

#### [NEW] [src/router/index.ts](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/router/index.ts)
- Routes:
  - `/login` → `LoginView.vue`
  - `/` → `Layout.vue` (requires auth)
    - `/chat` → `ChatView.vue`
    - `/documents` → `DocList.vue`
- Navigation guards: Redirect to login if not authenticated

---

### View Components

#### [NEW] [src/views/Login.vue](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/views/Login.vue)
- Element Plus form with username and password fields
- Form validation
- Login button that calls `userStore.login()`
- Error handling with message display
- Auto-redirect to `/chat` on success

#### [NEW] [src/views/Layout.vue](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/views/Layout.vue)
- Element Plus Container layout
- **Header**: 
  - Application title
  - User dropdown menu (Profile, Logout)
- **Sidebar**:
  - Navigation menu (Chat, Documents)
  - Active route highlighting
- **Main Content**: `<router-view />`
- Responsive design for mobile/desktop

#### [NEW] [src/views/Chat/ChatView.vue](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/views/Chat/ChatView.vue)
- Split pane layout:
  - Left: Session history list
  - Right: Chat message area + input box
- Features:
  - Auto-scroll to latest message
  - Streaming message display
  - Markdown rendering support (optional enhancement)

#### [NEW] [src/views/Doc/DocList.vue](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/views/Doc/DocList.vue)
- Element Plus Table displaying documents
- Columns: Filename, Upload Date, File Size, Actions
- Upload button with file picker
- Download and Delete actions per row
- Loading states and error handling

---

### Main Application Files

#### [NEW] [src/main.ts](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/main.ts)
- Create Vue app instance
- Register Element Plus
- Register Pinia
- Register Vue Router
- Mount application

#### [NEW] [src/App.vue](file:///e:/CS/Projs/LLM-Research-Assistant/frontend/src/App.vue)
- Root component with `<router-view />`
- Global styles
- Element Plus theme customization

---

## Verification Plan

### Automated Tests
```bash
# Start all services
1. docker-compose up -d  # MySQL, Redis, MinIO
2. # Start Java backend on port 8080
3. .\scripts\run_ai_service.bat  # Start AI service on port 8000
4. npm run dev  # Start frontend on port 5173
```

### Manual Verification

#### ✅ Project Setup
- [ ] `npm run dev` starts without errors
- [ ] Browser opens at `http://localhost:5173`
- [ ] No console errors in browser DevTools

#### ✅ Login Flow
- [ ] Navigate to `/login`
- [ ] Enter valid credentials (from backend user table)
- [ ] Verify token stored in localStorage
- [ ] Auto-redirect to `/chat` after login
- [ ] Verify 401 response redirects to `/login`

#### ✅ API Proxy
- [ ] Open Network tab in DevTools
- [ ] Trigger login API call
- [ ] Verify request goes to `http://localhost:5173/api/auth/login`
- [ ] Verify backend receives request at `http://localhost:8080/api/auth/login`
- [ ] Check Authorization header contains JWT token

#### ✅ Layout & Navigation
- [ ] Header displays correctly
- [ ] Sidebar navigation works
- [ ] User dropdown shows username
- [ ] Logout clears token and redirects to login
- [ ] Responsive design works on mobile viewport

#### ✅ Integration Readiness
- [ ] All routes are accessible
- [ ] No TypeScript compilation errors
- [ ] Vite build succeeds: `npm run build`
- [ ] Ready for Chat and Document module implementation

---

## Completed Features
- [x] Chat Module: Real-time SSE streaming, Session Management, Chat History (Sidebar, Rename, Delete)
- [x] UI Polish: Glassmorphism, Premium Bubbles, Floating Input, Markdown Rendering

## Next Steps
1. **Document Module**: File upload with progress, document viewer
2. **Testing**: Unit tests with Vitest, E2E tests with Cypress
3. **Optimizations**: Virtual scrolling for long chat lists (current scrollbar is basic)
