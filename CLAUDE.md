# Embabel 智能图书借阅 Agent

## 项目概述
基于 Embabel OODA 循环驱动的智能图书借阅 Agent。用户通过自然语言输入，Agent 自主完成意图理解 → 图书搜索 → 可用性校验 → 防重复过滤 → 借书执行的全链路。

## 技术栈
- **后端框架**：Spring Boot 3.x
- **模板引擎**：Thymeleaf + htmx（非前后端分离）
- **数据库**：H2 内存数据库（开发期），Spring Data JPA
- **LLM**：DeepSeek API（deepseek-chat 模型）
- **构建工具**：Maven
- **Java 版本**：21
- **UI 框架**：Bootstrap 5

## 项目结构
```
├── pom.xml
└── src/main/java/com/example/library/
    ├── LibraryApplication.java          # Spring Boot 入口
    ├── controller/
    │   └── LibraryController.java       # Thymeleaf Controller
    ├── agent/
    │   └── LibraryAgent.java            # OODA 循环编排
    ├── domain/                          # Java record，不含注解
    │   ├── Book.java
    │   ├── User.java
    │   ├── BorrowRequest.java
    │   └── ParsedQuery.java
    ├── action/                          # 每个 Action 一个类
    │   ├── ParseQueryAction.java
    │   ├── SearchBooksAction.java
    │   ├── CheckAvailabilityAction.java
    │   ├── FilterBorrowedBooksAction.java
    │   ├── ExecuteBorrowAction.java
    │   └── ReturnResultAction.java
    ├── condition/                       # 每个 Condition 一个类
    │   ├── HasValidQueryCondition.java
    │   ├── IsAvailableCondition.java
    │   └── NotBorrowedBeforeCondition.java
    ├── entity/                          # JPA Entity
    │   ├── BookEntity.java
    │   ├── UserEntity.java
    │   └── BorrowRecordEntity.java
    ├── repository/                      # Spring Data JPA Repository
    │   ├── BookRepository.java
    │   ├── UserRepository.java
    │   └── BorrowRecordRepository.java
    ├── service/
    │   ├── DeepSeekService.java         # DeepSeek API 调用封装
    │   └── LibraryService.java          # 视图层服务，组装 Controller 需要的数据
    └── config/
        └── DemoDataLoader.java          # 预置演示数据
```

## 架构规范

### OODA 循环顺序
Observe → Orient(ParseQuery) → Decide(SearchBooks → CheckAvailable → FilterBorrowed) → Act(ExecuteBorrow → ReturnResult)

### Action 命名规范
- 类名：`XxxAction`，放在 `action/` 包
- 方法名：`execute`，接受领域对象参数，返回领域对象
- 标注：`@Action(cost = N)` 注解（cost 根据复杂度：LLM=5, DB查询=2, 写入=1, 内存=0）

### Condition 命名规范
- 类名：`XxxCondition`，放在 `condition/` 包
- 方法名：`test`，返回 boolean
- 标注：`@Condition` 注解

### Domain 命名规范
- 使用 Java `record`
- 不可变，无注解
- 包路径：`domain/`

### Entity 命名规范
- 类名：`XxxEntity`，放在 `entity/` 包
- 使用 JPA 注解
- 包含 `toDomain()` 方法转领域对象

### 分层依赖规则（重要）
各层只能依赖紧邻的下一层，不可跨层调用：

```
Controller → LibraryService → LibraryAgent → Action/Repository
  ↑ HTTP/视图           ↑ 数据组装        ↑ OODA 编排     ↑ 原子操作
```

- **Controller**：只注入 `LibraryService`，不注入任何 Repository、Agent 或 Action
- **Service**：组装视图数据（图书列表、用户列表、借阅历史），调用 Agent
- **Agent**：编排 OODA 循环，调用 Action 和 Repository
- **Action**：注入 Condition 和 Repository，实现单个业务步骤

### Controller 规范
- 使用 `@Controller`（非 `@RestController`），返回模板视图
- 借书接口：`POST /borrow`，返回 HTML 片段（htmx）
- **禁止**在 Controller 中注入 Repository、EntityManager、Action

## 测试规范
- 测试框架：JUnit 5 + Mockito
- 集成测试：`@SpringBootTest` + `@AutoConfigureMockMvc`
- Condition 测试：纯逻辑，不依赖 Spring 上下文
- Action 测试：`@SpringBootTest`，Mock Repository
- Agent 测试：全链路集成测试，验证数据库状态变化

## DeepSeek API
- 模型：`deepseek-chat`
- 温度：0.1（低温度保证解析一致性）
- API Key：通过环境变量 `DEEPSEEK_API_KEY` 传入
- 无 key 时降级：使用简单关键词匹配（按 "," 分隔取第一个词作为 category）

## 演示数据
- 10 本图书：覆盖科幻、推理、文学、计算机、历史分类
- 2 个用户：张三（已借三体、百年孤独）、李四（未借任何书）
