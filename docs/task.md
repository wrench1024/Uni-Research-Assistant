# LLM-Research-Assistant 开发任务清单

## 阶段 1: 基础设施与环境配置
- [x] Docker Compose 环境搭建（MySQL, Redis, PostgreSQL+pgvector, MinIO）
- [x] 解决端口冲突问题（MySQL 3306 → 3307）
- [ ] Java 后端项目初始化（Spring Boot 3.x + Java 17）
- [ ] Python AI 服务项目结构优化
- [ ] 数据库表结构设计与初始化脚本
- [x] 添加项目 README 说明文档
- [x] 添加 .gitignore 配置文件

---

## 阶段 2: Java 核心后端开发（面试重点）

### 2.1 基础架构层
- [ ] 统一响应封装（`Result<T>` 类）
- [ ] 全局异常处理器（`GlobalExceptionHandler`）
- [ ] AOP 日志切面（记录请求/响应/耗时）
- [ ] 自定义业务异常类（`BizException`）
- [ ] 线程池配置（`ThreadPoolConfig`，体现 JUC 知识）

### 2.2 用户认证与鉴权模块
- [ ] 用户实体类与数据库表（User, Role, Permission）
- [ ] JWT Token 生成与验证工具类
- [ ] 登录/注册接口（Controller + Service + Mapper）
- [ ] 基于 Spring Security 的权限拦截器
- [ ] Redis 缓存 Token（防止重复登录）

### 2.3 文档管理模块
- [ ] 文档上传接口（支持 PDF/TXT/Markdown）
- [ ] 文件存储服务（MinIO 或本地存储）
- [ ] 文档元数据持久化（MySQL）
- [ ] 文档列表查询与分页（MyBatis-Plus）
- [ ] 文档删除与更新接口

### 2.4 AI 对话模块（核心业务）
- [ ] 对话会话管理（Session 表设计）
- [ ] WebSocket/SSE 实时通信（流式返回）
- [ ] 调用 Python AI 服务的 HTTP 客户端（OkHttp/RestTemplate）
- [ ] 异步任务处理（`CompletableFuture` + 线程池）
- [ ] 超时与重试机制（Resilience4j 或自定义）
- [ ] 对话历史持久化（MySQL）

### 2.5 RAG 检索增强模块
- [ ] 向量化任务提交接口
- [ ] 调用 Python 服务进行文档向量化
- [ ] 向量数据存储到 PostgreSQL+pgvector
- [ ] 检索相关文档片段接口
- [ ] 检索结果与 LLM 对话结合

---

## 阶段 3: Python AI 服务开发

### 3.1 项目结构重构
- [ ] 创建标准项目结构（`app/`, `services/`, `models/`, `utils/`）
- [ ] 配置管理（`.env` + Pydantic Settings）
- [ ] 日志配置（Loguru）

### 3.2 LLM 对话服务
- [ ] DeepSeek API 封装（支持流式响应）
- [ ] 对话上下文管理
- [ ] Prompt 模板设计
- [ ] 错误处理与降级策略

### 3.3 RAG 检索服务
- [ ] LlamaIndex 集成
- [ ] 文档加载器（PDF, TXT, Markdown）
- [ ] 文档分块策略（Chunk Size, Overlap）
- [ ] Embedding 模型配置（OpenAI 或本地模型）
- [ ] 向量存储到 PostgreSQL+pgvector
- [ ] 检索接口（Top-K 相似度搜索）

### 3.4 API 接口设计
- [ ] `/chat` - 对话接口（支持流式）
- [ ] `/embed` - 文档向量化接口
- [ ] `/search` - 向量检索接口
- [ ] 健康检查接口 `/health`

---

## 阶段 4: 前端开发（可选，时间充裕再做）
- [ ] 技术选型（Vue 3 + Element Plus 或 React + Ant Design）
- [ ] 登录/注册页面
- [ ] 文档管理页面（上传、列表、删除）
- [ ] 对话界面（支持流式显示）
- [ ] WebSocket/SSE 客户端集成

---

## 阶段 5: 测试与优化

### 5.1 单元测试
- [ ] Java Service 层单元测试（JUnit 5 + Mockito）
- [ ] Python Service 层单元测试（pytest）

### 5.2 集成测试
- [ ] Java 接口集成测试（MockMvc）
- [ ] Python 接口集成测试（TestClient）

### 5.3 性能优化
- [ ] Redis 缓存热点数据
- [ ] 数据库索引优化
- [ ] 线程池参数调优
- [ ] 接口响应时间监控

---

## 阶段 6: 复试准备

### 6.1 技术文档
- [ ] 系统架构图（画图工具：draw.io）
- [ ] 数据库 ER 图
- [ ] API 接口文档（Swagger/Knife4j）
- [ ] 部署文档

### 6.2 面试准备
- [ ] 整理项目亮点（AOP、线程池、分布式锁、RAG 原理）
- [ ] 准备技术难点讲解（如何处理并发、如何优化检索）
- [ ] 408 考点对应关系梳理
- [ ] 模拟面试问答

### 6.3 演示准备
- [ ] 准备演示数据（示例文档、对话案例）
- [ ] 录制演示视频
- [ ] PPT 制作

---

## 当前优先级（按顺序执行）
1. **Java 后端项目初始化**（创建 Spring Boot 项目骨架）
2. **数据库表结构设计**（用户表、文档表、对话表）
3. **基础架构层搭建**（统一响应、异常处理、AOP 日志）
4. **用户认证模块**（登录/注册 + JWT）
5. **Python AI 服务重构**（标准项目结构 + DeepSeek 集成）
