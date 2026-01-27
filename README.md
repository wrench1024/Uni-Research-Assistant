# LLM Research Assistant

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-4fc08d.svg)](https://vuejs.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.109.x-009688.svg)](https://fastapi.tiangolo.com/)
[![DeepSeek](https://img.shields.io/badge/AI-DeepSeek-blue)](https://www.deepseek.com/)

**LLM Research Assistant** 是一个专为学术研究设计的智能化辅助系统。结合了现代 Java 后端的稳健架构与 Python AI 服务的灵活性，通过 RAG（检索增强生成）技术，实现对本地文档的精准检索与智能化对话。

## 🌟 核心特性

### 1. 🤖 深度 AI 对话
- **流式响应**：毫秒级首字响应，配合打字机效果。
- **对话回溯与重试**：
  - **修改(Edit)**：支持修改历史提问，系统自动回滚后端状态，开启对话分支。
  - **重试(Retry)**：一键重新生成 AI 回复，前后端状态严格一致。
- **极致交互**：Enter 发送，Shift+Enter 换行；毛玻璃悬浮操作栏；动画丝滑。

### 2. 📚 RAG 知识库
- **全格式支持**：自动索引 `.pdf`, `.md`, `.txt`, `.doc`, `.docx`。
- **向量检索**：基于 `pgvector` 的高效语义检索。
- **自动同步**：文档上传即索引，删除即清理向量，无需手动维护。

### 3. 📁 文档管理系统
- **现代化 UI**：卡片式布局，文件类型图标识别。
- **对象存储**：集成 MinIO。
- **安全保障**：全 API 覆盖 JWT 认证，支持大文件上传（单文件 50MB）。
- **笔记管理**：支持阅读时的即时标注和笔记管理，支持标签系统。

### 4. 📝 智能研读与写作 (Enhanced)
- **多文档对比**：结构化生成对比表格，支持多维度深度分析。
- **混合检索 (RAG+)**：结合 BM25 关键词检索与向量语义检索，提升专业术语匹配精度。
- **引用生成**：自动提取元数据，生成 BibTeX 和 EndNote (RIS) 标准引用格式。

### 5. ✍️ 智能写作助手 (New)
- **多模式写作**：支持 **润色 (Polish)**、**扩写 (Expand)**、**纠错 (Fix Grammar)** 和 **续写 (Continue)**。
- **交互式体验**：选中文本即可唤起悬浮工具条，实时流式更新，所见即所得。
- **结构化导出**：生成的纯文本可自动解析为带目录和层级标题的 Word (`.docx`) 文档。
- **精准统计**：支持排除空格的中文字符统计。

## 🏗️ 架构概览

- **Frontend**: Vue 3 + TypeScript + Element Plus + Pinia + **Marked** + **Docx**
- **Backend**: Spring Boot 3 + MyBatis-Plus + MySQL + Redis + MinIO
- **AI Service**: FastAPI + LangChain + Gemini 2.0 Flash + PGVector + BM25/RRF
- **Infrastructure**: Docker Compose 一键启动中间件

## 🚀 快速开始

### 1. 基础环境
```bash
docker-compose up -d
```

### 2. AI 服务 (Python)
```bash
cd .
# 建议在 venv 中执行
pip install -r requirements.txt # 如果有的话
python main.py
```

### 3. 后端服务 (Java)
```bash
cd backend
mvn spring-boot:run
```

### 4. 前端界面 (Vue)
```bash
cd frontend
npm install
npm run dev
```

---
*Developed by wrench1024*
