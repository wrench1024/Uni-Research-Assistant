# LLM Research Assistant - 完整启动指南

## 📋 目录
1. [环境要求](#环境要求)
2. [首次启动步骤](#首次启动步骤)
3. [日常启动步骤](#日常启动步骤)
4. [验证服务](#验证服务)
5. [常见问题](#常见问题)
6. [停止服务](#停止服务)

---

## 🔧 环境要求

### 必需软件
- **Docker Desktop** (用于运行 MySQL、Redis、PostgreSQL、MinIO)
- **Java 17+** (后端服务)
- **Maven 3.6+** (Java 依赖管理)
- **Python 3.9+** (AI 服务，建议使用 venv)
- **Node.js 18+** (前端开发)
- **npm 9.0+** (前端依赖管理)
- **NVIDIA GPU (推荐)**: 显存 6GB+，安装 CUDA 12.1+ 驱动 (大幅提升 RAG 速度)

### 验证环境
```bash
# 检查 Docker
docker --version
docker-compose --version

# 检查 Java
java -version

# 检查 Maven
mvn -version

# 检查 Python
python --version

# 检查 Node.js 和 npm
node --version
npm --version
```

---

## 🚀 首次启动步骤

### 第一步：配置环境变量

1. **复制环境变量模板**
```bash
copy .env.template .env
```

2. **编辑 `.env` 文件，填入你的 API Key**
```env
# DeepSeek Official API Key
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Backend Secrets
JWT_SECRET=your_jwt_secret_here
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

### 第二步：启动 Docker 基础设施

```bash
# 启动所有中间件容器（MySQL、Redis、PostgreSQL、MinIO）
docker-compose up -d

# 等待 20-30 秒，让 MySQL 完全初始化
# 可以通过以下命令查看 MySQL 日志，确认启动完成
docker logs uni-research-mysql --tail 20
```

**验证容器状态：**
```bash
docker-compose ps
```

应该看到 4 个容器都在运行：
- `uni-research-mysql` (端口 3307)
- `uni-research-redis` (端口 16379)
- `uni-research-vector` (端口 5432)
- `uni-research-minio` (端口 19000, 19001)

### 第三步：初始化数据库

```bash
# 连接到 MySQL 容器
docker exec -it uni-research-mysql mysql -uroot -proot

# 在 MySQL 命令行中执行
USE uni_research_db;

# 创建用户表（示例）
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

# 插入测试用户（密码：123456，需要在后端用 BCrypt 加密）
INSERT INTO user (username, password, email) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@example.com');

# 退出 MySQL
EXIT;
```

### 第四步：安装 Python 依赖

```bash
# 1. 退出 Anaconda (如果使用了) - 避免 DLL 冲突
conda deactivate

# 2. 创建并激活虚拟环境（必须）
python -m venv venv
venv\Scripts\activate

# 安装核心依赖
pip install uvicorn fastapi python-multipart requests python-dotenv

# 安装 RAG 相关依赖（如果需要 RAG 功能）
# 3. 安装依赖 (包含 GPU 版 PyTorch)
# 注意：先安装 CUDA 版 PyTorch (这是关键)
pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121

# 4. 安装其他依赖
pip install -r requirements.txt
```

### 第五步：安装前端依赖

```bash
cd frontend
npm install
cd ..
```

### 第六步：编译后端项目

```bash
cd backend
mvn clean compile
cd ..
```

---

## ⚡ 日常启动步骤

### 启动顺序（重要！）

#### 1️⃣ 启动 Docker 容器（如果未运行）

```bash
# 检查容器状态
docker-compose ps

# 如果容器未运行，启动它们
docker-compose up -d

# 等待 20 秒让 MySQL 完全启动
```

#### 2️⃣ 启动 Python AI 服务（端口 8000）

**方式一：使用脚本（推荐）**
```bash
.\scripts\run_ai_service.bat
```

**方式二：手动启动**
```bash
# 激活虚拟环境
venv\Scripts\activate

# 启动 FastAPI 服务
python main.py
```

**验证：** 访问 http://localhost:8000 应该看到：
```json
{
  "status": "ok",
  "service": "LLM Research Assistant AI Service (DeepSeek)",
  "rag_enabled": true
}
```

#### 3️⃣ 启动 Java 后端服务（端口 8080）

**方式一：使用 Maven**
```bash
cd backend
mvn spring-boot:run
```

**方式二：使用 IDE**
- 在 IntelliJ IDEA 中打开 `backend` 文件夹
- 运行 `UniResearchApplication.java`

**验证：** 访问 http://localhost:8080/api/doc.html 查看 API 文档

#### 4️⃣ 启动 Vue 前端（端口 5173）

```bash
cd frontend
npm run dev
```

**验证：** 访问 http://localhost:5173 应该看到登录页面

---

## ✅ 验证服务

### 1. 检查所有服务状态

| 服务 | 端口 | 验证 URL | 预期响应 |
|------|------|----------|---------|
| **MySQL** | 3307 | `docker logs uni-research-mysql` | "ready for connections" |
| **Redis** | 16379 | `docker exec -it uni-research-redis redis-cli -a root ping` | "PONG" |
| **PostgreSQL** | 5432 | `docker logs uni-research-vector` | "database system is ready" |
| **MinIO** | 19000, 19001 | http://localhost:19001 | MinIO 控制台 |
| **Python AI** | 8000 | http://localhost:8000 | `{"status": "ok"}` |
| **Java Backend** | 8080 | http://localhost:8080/api/doc.html | Knife4j API 文档 |
| **Vue Frontend** | 5173 | http://localhost:5173 | 登录页面 |

### 2. 测试完整流程

1. **访问前端：** http://localhost:5173
2. **登录：** 使用 `admin` / `123456`
3. **测试对话：** 进入聊天页面，发送消息
4. **测试文档：** 上传一个 PDF 文件
5. **测试 RAG：** 上传文档后，询问文档相关问题

---

## ❓ 常见问题

### 问题 1：Python 提示 `ModuleNotFoundError: No module named 'uvicorn'`

**解决方案：**
```bash
# 激活虚拟环境
venv\Scripts\activate

# 安装依赖
pip install uvicorn fastapi python-multipart requests python-dotenv
```

### 问题 2：Java 后端报错 `Communications link failure`

**原因：** MySQL 容器未启动或未完全初始化

**解决方案：**
```bash
# 启动 Docker 容器
docker-compose up -d

# 等待 20-30 秒，检查 MySQL 日志
docker logs uni-research-mysql --tail 20

# 看到 "ready for connections" 后重启 Java 后端
```

### 问题 3：前端无法连接后端

**检查清单：**
1. Java 后端是否在 8080 端口运行？
2. 浏览器控制台是否有 CORS 错误？
3. `vite.config.ts` 中的代理配置是否正确？

**解决方案：**
```bash
# 检查后端是否运行
curl http://localhost:8080/api/doc.html

# 检查前端代理配置
# frontend/vite.config.ts 应该包含：
# proxy: {
#   '/api': {
#     target: 'http://localhost:8080',
#     changeOrigin: true
#   }
# }
```

### 问题 4：Docker 容器启动失败

**解决方案：**
```bash
# 停止所有容器
docker-compose down

# 清理旧容器和网络
docker system prune -f

# 重新启动
docker-compose up -d
```

### 问题 5：端口被占用

**检查端口占用：**
```bash
# Windows
netstat -ano | findstr :8080
netstat -ano | findstr :8000
netstat -ano | findstr :5173
netstat -ano | findstr :3307

# 杀死进程（替换 <PID> 为实际进程 ID）
taskkill /PID <PID> /F
```

### 问题 6：RAG 功能不可用

**检查清单：**
1. PostgreSQL (pgvector) 是否运行？
2. Python 是否安装了 `langchain`、`chromadb`、`pgvector`？
3. `.env` 文件中是否配置了 API Key？

**解决方案：**
```bash
# 检查 PostgreSQL
docker logs uni-research-vector

# 安装 RAG 依赖
pip install langchain langchain-google-genai chromadb sentence-transformers psycopg2-binary pgvector
```

---

## 🛑 停止服务

### 停止所有服务

```bash
# 1. 停止前端（在前端终端按 Ctrl+C）

# 2. 停止 Java 后端（在后端终端按 Ctrl+C）

# 3. 停止 Python AI 服务（在 Python 终端按 Ctrl+C）

# 4. 停止 Docker 容器
docker-compose down
```

### 仅停止 Docker 容器（保留数据）

```bash
docker-compose stop
```

### 完全清理（删除数据）

```bash
# ⚠️ 警告：这会删除所有数据库数据！
docker-compose down -v
```

---

## 📝 开发建议

### 推荐的终端布局

建议使用 4 个终端窗口：

1. **终端 1 - Docker**
   ```bash
   docker-compose up
   ```

2. **终端 2 - Python AI**
   ```bash
   venv\Scripts\activate
   python main.py
   ```

3. **终端 3 - Java Backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

4. **终端 4 - Vue Frontend**
   ```bash
   cd frontend
   npm run dev
   ```

### 日志查看

```bash
# Docker 容器日志
docker logs -f uni-research-mysql
docker logs -f uni-research-redis
docker logs -f uni-research-vector

# Python 日志（在运行终端查看）

# Java 日志（在运行终端查看，或查看 backend/backend.log）

# 前端日志（在浏览器控制台查看）
```

---

## 🎯 快速启动脚本（可选）

创建一个 `start-all.bat` 脚本：

```batch
@echo off
echo Starting LLM Research Assistant...

echo [1/4] Starting Docker containers...
docker-compose up -d
timeout /t 25 /nobreak

echo [2/4] Starting Python AI Service...
start cmd /k "venv\Scripts\activate && python main.py"
timeout /t 5 /nobreak

echo [3/4] Starting Java Backend...
start cmd /k "cd backend && mvn spring-boot:run"
timeout /t 15 /nobreak

echo [4/4] Starting Vue Frontend...
start cmd /k "cd frontend && npm run dev"

echo.
echo ✅ All services started!
echo.
echo Access the application at: http://localhost:5173
echo API Documentation: http://localhost:8080/api/doc.html
echo.
pause
```

---

## 📞 技术支持

如遇到问题，请检查：
1. 本文档的「常见问题」章节
2. 项目 `docs/` 目录下的其他文档
3. 各服务的日志输出

---

**祝你开发顺利！🚀**

*Last Updated: 2026-01-22*
