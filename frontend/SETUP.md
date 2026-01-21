# Frontend Setup Guide

## Overview

This document provides instructions for setting up and running the Vue 3 + TypeScript frontend for the LLM-Research-Assistant project.

## Prerequisites

- Node.js v18 or higher
- npm 9.0 or higher
- Backend services running (Java Backend on port 8080, Python AI Service on port 8000)

## Project Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── api/               # API service layer
│   │   ├── request.ts    # Axios instance with interceptors
│   │   ├── auth.ts       # Authentication APIs
│   │   ├── chat.ts       # Chat APIs (SSE streaming)
│   │   └── doc.ts        # Document management APIs
│   ├── assets/            # Images, fonts, etc.
│   ├── components/        # Reusable Vue components
│   ├── router/            # Vue Router configuration
│   │   └── index.ts      # Routes and navigation guards
│   ├── stores/            # Pinia state management
│   │   ├── userStore.ts  # User authentication state
│   │   └── chatStore.ts  # Chat session state
│   ├── views/             # Page components
│   │   ├── Login.vue     # Login page
│   │   ├── Layout.vue    # Main layout (sidebar + header)
│   │   ├── Chat/
│   │   │   └── ChatView.vue  # AI chat interface
│   │   └── Doc/
│   │       └── DocList.vue   # Document management
│   ├── App.vue            # Root component
│   ├── main.ts            # Application entry point
│   └── style.css          # Global styles
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.app.json
└── vite.config.ts         # Vite configuration (with API proxy)
```

## Installation

Navigate to the frontend directory and install dependencies:

```bash
cd frontend
npm install
```

## Development Server

Start the development server:

```bash
npm run dev
```

The application will be available at `http://localhost:5173`

## API Proxy Configuration

Vite is configured to proxy `/api` requests to the backend:

- **Frontend**: `http://localhost:5173/api/*`
- **Backend**: `http://localhost:8080/api/*`

This is configured in `vite.config.ts`:

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      ws: true
    }
  }
}
```

## Key Features

### 1. Authentication
- Login page with form validation
- JWT token storage in localStorage
- Automatic token injection in API requests
- Route guards for protected pages
- Auto-redirect on 401 errors

### 2. State Management (Pinia)

#### User Store
- User authentication state
- Login/logout actions
- Token persistence
- User info management

#### Chat Store
- Current session management
- Message history
- SSE streaming support
- Session CRUD operations

### 3. Routing

The application has the following routes:

- `/login` - User login (public)
- `/` - Main layout (requires auth)
  - `/chat` - AI chat interface
  - `/documents` - Document management

### 4. API Layer

All API calls use Axios with:
- Base URL: `/api` (proxied to backend)
- Request interceptor: JWT token injection
- Response interceptor: Error handling and authentication checks

## Building for Production

Build the application:

```bash
npm run build
```

Build output will be in the `dist/` directory.

Preview production build:

```bash
npm run preview
```

## Testing the Application

1. **Start all services:**
   ```bash
   # Terminal 1: Start Docker services
   docker-compose up -d

   # Terminal 2: Start Java backend (port 8080)
   cd backend
   mvn spring-boot:run

   # Terminal 3: Start Python AI service (port 8000)
   .\scripts\run_ai_service.bat

   # Terminal 4: Start frontend (port 5173)
   cd frontend
   npm run dev
   ```

2. **Access the application:**
   - Open browser to `http://localhost:5173`
   - You should see the login page

3. **Test login:**
   - Use credentials from your backend user database
   - On success, you'll be redirected to `/chat`
   - Token should be stored in localStorage

4. **Test features:**
   - Navigate between Chat and Documents pages
   - Send a chat message (verify SSE streaming)
   - Upload, download, and delete documents
   - Logout and verify token is cleared

## Environment Variables

No environment variables are required for the frontend. All configuration is in `vite.config.ts`.

## Common Issues

### Port Already in Use
If port 5173 is already in use, Vite will automatically try the next available port, or you can specify a different port in `vite.config.ts`.

### API Proxy Not Working
- Ensure the backend is running on port 8080
- Check browser DevTools Network tab
- Verify requests are being proxied correctly

### TypeScript Errors
- Run `npm install` to ensure all dependencies are installed
- Check `tsconfig.json` and `tsconfig.app.json` for proper configuration

### Element Plus Icons Not Showing
Element Plus icons are imported as needed in components. Ensure `@element-plus/icons-vue` is installed.

## Next Steps

After the basic setup is complete, consider:

1. **Testing**: Add unit tests with Vitest and E2E tests with Cypress
2. **Documentation**: Add JSDoc comments to functions
3. **UI Enhancements**: 
   - Markdown rendering for chat messages
   - Syntax highlighting for code blocks
   - Document preview
4. **Performance**: Code splitting, lazy loading, caching strategies
5. **Accessibility**: ARIA labels, keyboard navigation
6. **Internationalization**: i18n support for multiple languages

## Technologies Used

- **Vue 3** - Progressive JavaScript framework
- **TypeScript** - Type-safe JavaScript
- **Vite** - Fast build tool and dev server
- **Element Plus** - Vue 3 UI component library
- **Axios** - HTTP client
- **Pinia** - State management
- **Vue Router** - Client-side routing
- **Sass** - CSS preprocessor

## Support

For issues or questions, please refer to:
- Project documentation in `docs/`
- Backend API documentation
- Element Plus docs: https://element-plus.org
- Vue 3 docs: https://vuejs.org
