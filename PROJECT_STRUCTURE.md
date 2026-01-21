# LLM Research Assistant - Project Structure

## 📁 Directory Overview

```
LLM-Research-Assistant/
├── backend/                    # Spring Boot 后端服务
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/uni/research/
│   │   │   │   ├── common/           # 公共组件
│   │   │   │   │   ├── config/       # 配置类 (Security, Redis, Thread Pool, etc.)
│   │   │   │   │   ├── exception/    # 全局异常处理
│   │   │   │   │   ├── filter/       # JWT 认证过滤器
│   │   │   │   │   ├── result/       # 统一响应格式
│   │   │   │   │   ├── service/      # MinIO 服务
│   │   │   │   │   └── util/         # JWT 工具类
│   │   │   │   ├── module/           # 业务模块
│   │   │   │   │   ├── auth/         # 认证模块 (注册/登录/登出)
│   │   │   │   │   ├── chat/         # AI 对话模块
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── mapper/
│   │   │   │   │   │   └── service/
│   │   │   │   │   ├── doc/          # 文档管理模块
│   │   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── mapper/
│   │   │   │   │   │   └── service/
│   │   │   │   │   └── user/         # 用户模块
│   │   │   │   └── UniResearchApplication.java  # 启动类
│   │   │   └── resources/
│   │   │       ├── application.yml          # 主配置文件
│   │   │       ├── application-dev.yml      # 开发环境配置
│   │   │       └── db/
│   │   │           └── init.sql             # 数据库初始化脚本
│   │   └── test/                   # 测试代码
│   ├── logs/                       # 应用日志目录 (gitignored)
│   ├── pom.xml                     # Maven 依赖配置
│   └── PROJECT_STRUCTURE.md        # 后端详细结构文档
│
├── docs/                           # 项目文档
│   ├── implementation_plan.md      # 实现计划
│   ├── task.md                     # 任务清单
│   ├── walkthrough.md              # 开发历程
│   └── SESSION_HANDOFF.md          # 会话交接文档
│
├── scripts/                        # 脚本目录
│   ├── system-test.ps1             # 系统综合测试脚本
│   ├── run_ai_service.bat          # Python AI 服务启动脚本
│   └── test_gemini.ps1             # AI 服务独立测试脚本
│
├── data/                           # Docker 数据目录 (gitignored)
│   ├── mysql/                      # MySQL 数据
│   ├── redis/                      # Redis 数据
│   ├── minio/                      # MinIO 对象存储
│   └── pgvector/                   # PGVector 向量数据库
│
├── venv/                           # Python 虚拟环境 (gitignored)
│
├── docker-compose.yml              # Docker Compose 配置
├── main.py                         # Python AI 服务 (FastAPI + Gemini)
├── README.md                       # 项目说明
└── .gitignore                      # Git 忽略文件配置

```

## 🔧 Core Technologies

### Backend (Java)
- **Framework**: Spring Boot 3.2.1
- **Security**: Spring Security 6.2.1 + JWT
- **ORM**: MyBatis-Plus 3.5.5
- **Database**: MySQL 8.0
- **Cache**: Redis (Lettuce)
- **Object Storage**: MinIO
- **API Doc**: Knife4j (OpenAPI 3)

### Frontend (Planned)
- Framework: Vue 3 / React (TBD)

### AI Service (Planned)
- Framework: FastAPI (Python)
- LLM Integration: OpenAI / Custom Models

## 📝 Important Files

| File | Description |
|------|-------------|
| `backend/pom.xml` | Maven dependencies and build configuration |
| `backend/src/main/resources/application-dev.yml` | Development environment config (DB, Redis, JWT, MinIO) |
| `backend/src/main/resources/db/init.sql` | Database schema initialization |
| `docker-compose.yml` | Container orchestration (MySQL, Redis, MinIO, PGVector) |
| `docs/task.md` | Development task checklist |
| `scripts/system-test.ps1` | Comprehensive backend API testing script |

## 🚀 Quick Start

### 1. Start Infrastructure Services
```bash
docker-compose up -d
```

### 2. Run Backend
```bash
cd backend
mvn spring-boot:run
```

### 3. Run Tests
```powershell
.\scripts\system-test.ps1
```

### 4. Access API Documentation
http://localhost:8080/api/doc.html

## 📊 Module Status

| Module | Status | Endpoints |
|--------|--------|-----------|
| **Authentication** | ✅ Complete | `/api/auth/register`, `/api/auth/login`, `/api/auth/logout` |
| **Document Management** | ✅ Complete | `/api/doc/upload`, `/api/doc/list`, `/api/doc/download`, `/api/doc/delete` |
| **AI Chat** | ✅ Complete | `/api/chat/session`, `/api/chat/send` (Real LLM) |
| **Python AI Service** | ✅ Complete | `main.py` (FastAPI + Gemini 2.5 Flash) |
| **Frontend** | ⏳ Planned | Vue/React interface |

## 🧪 Testing

- **System Test**: `scripts/system-test.ps1` - Comprehensive API testing (15 test cases)
- **Coverage**: 93.3% pass rate
- **Manual Testing**: Use Knife4j UI at `/api/doc.html`

## 📦 Build & Deploy

### Development
```bash
mvn spring-boot:run
```

### Production Build
```bash
mvn clean package -DskipTests
java -jar target/uni-research-assistant-1.0.0.jar
```

---

**Last Updated**: 2026-01-19  
**Version**: 1.0.0  
**Maintainer**: wrench1024
