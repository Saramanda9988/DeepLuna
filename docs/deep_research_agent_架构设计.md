# Deep Research Agent — 精简单体架构设计（MVP-1）

> 本文是基于前期的分布式架构思考重新生成的 **单体版本 Deep Research Agent 架构文档**。目标是在 Java 技术栈下实现一个可运行、可扩展的 Deep Research Agent 系统，采用 **线程池 + 异步任务编排 + Spring 事件驱动机制** 实现多 Agent 协作与状态流转。

---

## 🎯 目标

1. 支持从用户输入到报告输出的完整 research 流程。
2. 使用轻量级单体架构（Spring Boot）
3. 通过 **线程池（CompletableFuture）** 并行运行多个 Sub-agent。
4. 使用 **Spring ApplicationEventPublisher** 或 **Guava EventBus** 驱动任务状态流转。
5. 代码层次清晰，便于未来扩展到分布式架构。

---

## 🧱 架构总览

**核心组件（单体架构）**：

1. **API 层** — 提供 REST 与 WebSocket 接口，管理 Research Session。
2. **Supervisor（协调器）** — 管理任务生命周期、派发给 Sub-agent 并监听事件。
3. **Sub-agent 池** — 多个异步执行单元，完成信息检索、分析、总结等任务。
4. **事件总线（EventBus / Spring ApplicationEventPublisher）** — 负责任务结果的状态流转。
5. **存储层** — PostgreSQL（任务状态、研究记录） + 内存缓存。
6. **报告生成器** — 汇总结果生成 DOCX 报告（Apache POI）。

---

## 🧩 模块职责

### 1. API 层

- 框架：Spring Boot + Spring MVC + WebSocket
- 接口示例：
  - `POST /v1/research/start` — 启动研究任务
  - `GET /v1/research/{id}/status` — 查询研究进度
  - `WS /v1/research/{id}` — 实时推送状态事件（如 Clarifying、Retrieving、Analyzing...）

### 2. Supervisor

- 负责 orchestrate 全流程，核心逻辑：
  - 接收 Clarifier 的结果（ResearchBrief）
  - 使用线程池提交多个 Sub-agent 任务（如 RetrieverAgent、AnalyzerAgent）
  - 监听 Sub-agent 执行结果事件并驱动状态流转。

示例：
```java
@Component
public class Supervisor {
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final ApplicationEventPublisher publisher;

    public void startResearch(ResearchBrief brief) {
        CompletableFuture.supplyAsync(() -> retriever.retrieve(brief), pool)
            .thenAccept(result -> publisher.publishEvent(new RetrievalResultEvent(result)));
    }

    @EventListener
    public void onRetrievalResult(RetrievalResultEvent event) {
        CompletableFuture.supplyAsync(() -> analyzer.analyze(event.getData()), pool)
            .thenAccept(result -> publisher.publishEvent(new AnalysisResultEvent(result)));
    }
}
```

### 3. Sub-agent 池

- 每个 Agent 实现统一接口：
```java
public interface AgentWorker {
    TaskResult execute(Task task);
}
```
- 示例 Agents：
  - **ClarifierAgent** — 问题澄清与分解。
  - **RetrieverAgent** — 信息检索。
  - **AnalyzerAgent** — 信息分析与总结。
  - **SynthesisAgent** — 整合多源结果。
  - **ReportAgent** — 生成报告。

- 执行策略：基于 `CompletableFuture` 的异步流水线或并行执行。

### 4. 事件总线（Event Flow）

使用 **Spring ApplicationEventPublisher** 实现解耦的状态流转。

- 事件类型：
  - `ResearchStartedEvent`
  - `ClarificationCompleteEvent`
  - `RetrievalResultEvent`
  - `AnalysisResultEvent`
  - `ReportGeneratedEvent`

- 流转机制：每个事件触发下一个 Agent 的执行。

示意图：
```
User → Clarifier → (event) → Retriever → (event) → Analyzer → (event) → Reporter → Output
```

### 5. 存储与状态管理

- 使用 SQLite / PostgreSQL 记录：
  - 会话状态（INIT, CLARIFYING, RUNNING, REPORTING, COMPLETED）
  - 每个任务的输入 / 输出 / 状态。

```sql
CREATE TABLE research_session (
  id UUID PRIMARY KEY,
  title TEXT,
  status TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE task (
  id UUID PRIMARY KEY,
  session_id UUID REFERENCES research_session(id),
  type TEXT,
  payload JSONB,
  status TEXT,
  result JSONB
);
```

### 6. 报告生成

- 汇总所有 Agent 结果生成结构化报告。
- 使用 Apache POI / docx4j 输出 `.docx` 文件。

---

## 🔄 执行流程（序列图）

```text
User
 └─> API: /v1/research/start
      └─> Supervisor.startResearch()
            ├─ ClarifierAgent.clarify()
            │   └─ publish ClarificationCompleteEvent
            ├─ RetrieverAgent.retrieve()
            │   └─ publish RetrievalResultEvent
            ├─ AnalyzerAgent.analyze()
            │   └─ publish AnalysisResultEvent
            └─ ReportAgent.generate()
                └─ publish ReportGeneratedEvent
```

所有事件由 Spring 框架内部异步调用，形成非阻塞流水线。

---

## ⚙️ 并发与编排机制

- 使用 `ExecutorService` 控制线程数。
- 使用 `CompletableFuture` 实现依赖链与结果回调。
- EventBus 负责解耦模块与任务状态流。

示例（并行子任务）：
```java
CompletableFuture<List<Evidence>> wikiTask =
    CompletableFuture.supplyAsync(() -> retriever.retrieveFromWiki(brief), pool);
CompletableFuture<List<Evidence>> paperTask =
    CompletableFuture.supplyAsync(() -> retriever.retrieveFromPapers(brief), pool);

CompletableFuture.allOf(wikiTask, paperTask)
    .thenApply(v -> combine(wikiTask.join(), paperTask.join()))
    .thenAccept(data -> analyzer.analyze(data));
```

---

## 🧮 状态机

| 阶段 | 说明 |
|-------|------|
| INIT | 创建会话 |
| CLARIFYING | 问题澄清中 |
| RUNNING | 执行研究任务中 |
| REPORTING | 报告生成中 |
| COMPLETED | 已完成 |
| FAILED | 异常中断 |

---

## 📦 技术选型（简化）

| 模块 | 技术 |
|------|------|
| 主框架 | Spring Boot 3.x |
| 异步执行 | Java CompletableFuture + ExecutorService |
| 事件流转 | Spring ApplicationEventPublisher / Guava EventBus |
| 数据存储 | SQLite / PostgreSQL |
| 文档处理 | Apache Tika / Jsoup |
| 报告导出 | Apache POI / docx4j |
| 日志 | SLF4J + Logback |

---

## 🧭 开发路线

1. **阶段一（MVP）**
   - 完成 Clarifier → Retriever → Analyzer → Reporter 的单流程闭环。
   - 实现事件驱动的状态流转。
   - 导出 `.docx` 报告。

2. **阶段二**
   - 支持多来源并行检索。
   - 引入 Redis 缓存与日志查询接口。

3. **阶段三（演进）**
   - 将事件总线替换为 RabbitMQ。
   - Sub-agent 拆分为独立微服务，通过 gRPC 通信。

---

## 💡 小结

本设计在保持 **模块职责清晰** 与 **易扩展性** 的同时，去除了不必要的分布式复杂度。
通过线程池 + 事件机制，可以在单体项目中实现接近消息驱动的 Agent 协作模型，为未来的分布式版本奠定架构基础。

