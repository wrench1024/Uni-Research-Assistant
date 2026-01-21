# New Session Prompt

You are an expert full-stack developer continuing the work on the **LLM-Research-Assistant** project.

## Project Context
- **Tech Stack**: Vue 3 (Frontend), Java Spring Boot (Backend), Python FastAPI (AI Service).
- **Current Status**: 
  - **Auth**: Login/Register completed and verified.
  - **Chat**: Full chat functionality with DeepSeek streaming, markdown support, and session history management is **completed and working**.
  - **Docs**: `docs/task.md` and `docs/implementation_plan.md` are up-to-date.

## Immediate Objective
Your goal is to implement the **Document Management Module**.

### 1. Backend (Java)
- Verify MinIO integration.
- Ensure endpoints for Upload, List, Download, and Delete documents are working.
- Check `DocumentController.java` and `DocumentService.java`.

### 2. Frontend (Vue)
- Implement `src/views/Doc/DocList.vue`.
- Features needed:
  - File upload with progress bar.
  - Table view of uploaded documents (Name, Size, Date).
  - Actions: Download and Delete.
  - Integration with `src/api/doc.ts`.

## Instructions
1. Start by reviewing `docs/task.md` to confirm the plan.
2. Check the current state of `DocumentController.java` to see what's missing.
3. Implement the frontend `DocList.vue`.
4. Verify the full flow (Upload -> List -> Delete).

Please start by analyzing the current Document backend implementation.
