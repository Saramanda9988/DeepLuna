# DeepLuna

一个基于 Spring Boot 和 Spring AI 的多 agent 研究与检索系统。它编排研究工作流（监督 Agent + 子 Agent）、执行网页/搜索、文档摄取与切分、生成 embeddings、将向量存储到 PostgreSQL/pgvector，并对外提供聊天、会话、模型与用户等清晰的 REST API。

## 技术栈

- 语言 / 运行时：Java 21（Eclipse Temurin）
- 框架：Spring Boot 3.2.x
- AI：Spring AI 1.1.0-M4（集成 OpenAI、DeepSeek、ZhipuAI；pgvector 向量存储）
- 数据：PostgreSQL（带 pgvector）、Spring Data JPA、Hibernate
- API 文档：Springdoc OpenAPI + Knife4j
- 工具库：Lombok、Hutool、Guava、Netty、Apache POI、Apache Tika
- 构建：Gradle 8+
- 容器：Docker、Docker Compose

## 本地构建与运行

```bash
# 构建（跳过测试）
gradle clean bootJar -x test

# 运行
gradle bootRun

# 测试
gradle test
```

### 使用 Docker 部署

#### 1. 确认已安装 Docker 与 Docker Compose

（可选）在项目根目录创建一个 `.env` 文件以存放密钥：

```env
DEEPSEEK_API_KEY=your-deepseek-key
ZHIPUAI_API_KEY=your-zhipuai-key
WEBSEARCH_TAVILY_API_KEY=your-tavily-key
```

#### 2. 启动服务

```bash
docker-compose up -d
```

注意：
- 数据库初始化脚本位于 `src/main/resources/db/migration/V1__.sql`（包含 chat/session/task/user/document_metadata 等表）
- 使用 `ankane/pgvector` 镜像以启用向量扩展
- 配置采用两层：
  - `application.yml`：统一结构（字段保持一致）
  - `application-{profile}.properties`：按环境注入值（如 `local` / `prod`）
- 默认 profile 为 `local`，可用环境变量 `SPRING_PROFILES_ACTIVE` 切换。

## Roadmap

- Agent 与编排
  - 可插拔工具（网页搜索、爬取、摘要、报告生成）
  - spring event 解耦主从agent
- 检索与存储
  - 更高级的分块策略 + 混合检索（BM25 + 向量），回答更精准
  - 支持外部对象存储（MinIO / S3）用于文件存储
  - 可选使用rag工具
- 可观测性与运维
  - 前端websocket指标/追踪面板；结构化日志；限流
  - 使用token的计数
- 安全与多租户
  - JWT 认证、RBAC；租户隔离与配额
- 产品体验
  - 在 `webapp/` 下开发 Web UI 用于管理会话、文档与聊天
  - 提示模板与模型路由策略
