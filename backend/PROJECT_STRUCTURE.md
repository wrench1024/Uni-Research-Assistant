# Uni-Research-Assistant 项目结构

## 📁 当前目录结构

```
backend/
├── pom.xml                                    # Maven 配置文件
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/uni/research/
    │   │       └── UniResearchApplication.java    # 启动类
    │   └── resources/
    │       ├── application.yml                # 主配置文件
    │       ├── application-dev.yml            # 开发环境配置
    │       ├── db/                            # 数据库脚本目录
    │       └── mapper/                        # MyBatis XML 目录
    └── test/
        └── java/
            └── com/uni/research/              # 测试类目录
```

## 📋 已完成的配置

### 1. Maven 依赖 (pom.xml)
- ✅ Spring Boot 3.2.1
- ✅ Spring Security + JWT (0.12.3)
- ✅ MyBatis-Plus 3.5.5
- ✅ MySQL Connector
- ✅ Redis
- ✅ Lombok
- ✅ Hutool 5.8.25
- ✅ OkHttp 4.12.0
- ✅ Knife4j 4.4.0 (API 文档)

### 2. 应用配置 (application.yml)
- ✅ 应用名称: `uni-research-assistant`
- ✅ 服务端口: `8080`
- ✅ Context Path: `/api`
- ✅ Knife4j 中文文档

### 3. 开发环境配置 (application-dev.yml)
- ✅ MySQL 数据库: `uni_research_db` (端口 3307)
- ✅ Redis: localhost:16379 (密码 root)
- ✅ MyBatis-Plus 配置
- ✅ JWT 配置 (24小时过期)
- ✅ Python AI 服务地址: http://localhost:8000
- ✅ 日志级别配置

## 🎯 下一步需要创建的模块

### 阶段 1: 基础架构层 (common/)
```
com/uni/research/common/
├── config/                    # 配置类
│   ├── ThreadPoolConfig.java     # 线程池配置（面试加分项）
│   ├── RedisConfig.java           # Redis 配置
│   ├── MyBatisPlusConfig.java     # MyBatis-Plus 配置
│   └── WebConfig.java             # Web 配置（CORS等）
├── exception/                 # 异常处理
│   ├── BizException.java          # 业务异常
│   └── GlobalExceptionHandler.java # 全局异常处理器
├── aspect/                    # AOP 切面
│   └── ApiLogAspect.java          # API 日志切面（面试加分项）
├── result/                    # 统一响应
│   ├── Result.java                # 统一响应封装
│   └── ResultCode.java            # 响应码枚举
└── constant/                  # 常量
    └── RedisKeyConstant.java      # Redis Key 常量
```

### 阶段 2: 认证模块 (module/auth/)
```
com/uni/research/module/auth/
├── controller/
│   └── AuthController.java
├── service/
│   ├── AuthService.java
│   └── impl/
│       └── AuthServiceImpl.java
├── entity/
│   └── User.java
├── mapper/
│   └── UserMapper.java
└── dto/
    ├── LoginRequest.java
    ├── LoginResponse.java
    └── RegisterRequest.java
```

### 阶段 3: 文档管理模块 (module/doc/)
```
com/uni/research/module/doc/
├── controller/
│   └── DocumentController.java
├── service/
│   ├── DocumentService.java
│   └── impl/
│       └── DocumentServiceImpl.java
├── entity/
│   └── Document.java
├── mapper/
│   └── DocumentMapper.java
└── dto/
    ├── DocumentUploadRequest.java
    └── DocumentVO.java
```

### 阶段 4: 对话模块 (module/chat/)
```
com/uni/research/module/chat/
├── controller/
│   └── ChatController.java
├── service/
│   ├── ChatService.java
│   └── impl/
│       └── ChatServiceImpl.java
├── entity/
│   ├── ChatSession.java
│   └── ChatMessage.java
├── mapper/
│   ├── ChatSessionMapper.java
│   └── ChatMessageMapper.java
└── dto/
    ├── ChatRequest.java
    └── ChatResponse.java
```

## 🔑 关键配置说明

### MySQL 连接
- **数据库名**: `uni_research_db`
- **端口**: 3307 (释放本地 MySQL 占用即可)
- **用户名**: root
- **密码**: root

### Redis 连接
- **主机**: localhost
- **端口**: 16379
- **密码**: root
- **数据库**: 0

### JWT 配置
- **密钥**: 需在生产环境修改
- **过期时间**: 24小时
- **请求头**: Authorization
- **前缀**: Bearer

### Python AI 服务
- **地址**: http://localhost:8000
- **连接超时**: 5秒
- **读取超时**: 30秒

## 📝 面试考点对应

| 组件 | 408 考点 | 说明 |
|------|---------|------|
| ThreadPoolConfig | 操作系统-线程管理 | 自定义线程池参数，体现对 JUC 的理解 |
| ApiLogAspect | 设计模式-代理模式 | Spring AOP 基于动态代理实现 |
| GlobalExceptionHandler | 软件工程-异常处理 | 统一异常处理，提升系统健壮性 |
| JWT 认证 | 计算机网络-加密算法 | HMAC-SHA256 签名，保证 Token 安全 |
| Redis 缓存 | 计算机组成-存储层次 | 热点数据缓存，减少数据库压力 |
| MyBatis-Plus | 数据库-SQL 优化 | 使用索引，避免全表扫描 |

## ✅ 验证项目

运行以下命令验证项目配置：

```bash
cd backend
mvn clean compile
```

如果编译成功，说明 Maven 配置正确。

## 🚀 启动项目

```bash
cd backend
mvn spring-boot:run
```

启动成功后访问：
- API 文档: http://localhost:8080/api/doc.html
