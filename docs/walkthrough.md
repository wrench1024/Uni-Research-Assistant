# Chat Module Implementation Walkthrough

## Overview
This walkthrough covers the implementation of the backend core for the AI Chat Module. We have added the necessary data models, a service layer with Mock SSE support, and a controller to expose the chat functionality.

## Changes

### Database & Entities
- **New Entities**: `ChatSession`, `ChatMessage`
- **Mappers**: `ChatSessionMapper`, `ChatMessageMapper`
- **Tables**: `chat_session`, `chat_message` (Already existed in `init.sql`, mapped now)

### Service Layer (`ChatServiceImpl`)
- **Session Management**: Create new sessions, retrieve history.
- **Mock SSE Streaming**: `streamChat` method simulates an AI response by streaming a fixed text character-by-character using `SseEmitter`.
  - **Logic**: 
    1. Saves user message.
    2. Strings back a mock response.
    3. Saves full mock response to DB after streaming completes.

### API Layer (`ChatController`)
- `POST /api/chat/session`: Create a session.
- `GET /api/chat/session/{id}/messages`: Get message history.
- `POST /api/chat/send`: Send a message and receive `text/event-stream` response.

## Prerequisites

> [!IMPORTANT]
> 在运行验证脚本之前，请确保以下服务已启动：

1. **Docker Desktop 必须运行**
   - 本项目依赖 MySQL (端口 3307)、Redis、MinIO、pgvector
   - 启动命令：`docker-compose up -d`
   - 验证命令：`docker ps` （应看到 4 个容器在运行）

2. **后端服务必须启动**
   - 在 IDE 中启动 `UniResearchApplication`，或
   - 使用命令：`cd backend && mvn spring-boot:run`
   - 验证：访问 http://localhost:8080/api/doc.html 应该能看到 API 文档

## Verification

### Automated Verification Script
A PowerShell script `test-chat-sse.ps1` has been created in the project root.

**Usage:**
```powershell
./test-chat-sse.ps1
```

**Expected Output:**
1. Login success (Token received).
2. Session created.
3. Stream connected, receiving lines like:
   `这`, `是`, `模`, `拟`, ...
4. History verified (User message and Assistant message stored).

### Known Issues

**Maven Build Error (已解决)**  
During auto-verification, a `PluginResolutionException` occurred due to a corrupted `surefire-3.1.2.pom` in the local `.m2` repository. This was resolved by deleting the corrupted Maven plugins directory.

**API Path 403 Error (已解决)**  
通过以下两项改进已彻底解决：
1. **JDK 版本修复**：将 IDEA 项目编译级别改为 Java 17，确保了 Spring Boot 3.2 的正确运行。
2. **URL 映射修复**：移除了 Controller 中冗余的 `/api` 前缀，使其与 `context-path: /api` 完美配合，解决了路径重叠导致的 404/403 问题。

现在运行 `./test-chat-sse.ps1` 可以看到完整的登录、会话创建、消息发送及历史记录验证流程。

---
**Module Status**: ✅ Fully Functional (Mock Mode)
