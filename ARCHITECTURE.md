# Embabel 智能图书借阅 Agent — 概要设计

## 1. 项目概述

基于 Embabel 框架（OODA 循环驱动）构建一个**全栈**智能图书借阅 Agent，用户通过 Web 界面输入自然语言，Agent 自主完成意图理解 → 图书搜索 → 可用性校验 → 防重复过滤 → 借书执行的全链路。

**核心理念**：利用 OODA 循环（Observe → Orient → Decide → Act）应对动态环境——图书库存会变、用户借阅历史会变，每次请求都是完整的 OODA 闭环。

---

## 2. 系统架构（三层架构）

```
┌──────────────────────────────────────────────────────────────┐
│                    前端层 (Vue 3 + Element Plus)               │
│  图书浏览  │  借书对话框  │  借阅历史  │  搜索结果展示          │
└──────────────────────────┬───────────────────────────────────┘
                           │  REST API (JSON)
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                  后端层 (Spring Boot 3.x)                     │
│                                                              │
│  ┌──────────────┐  ┌──────────────────────────────────────┐  │
│  │  Controller   │  │     Embabel Agent 引擎 (OODA Loop)   │  │
│  │  (REST API)   │  │                                      │  │
│  │              │  │  [Observe] → [Orient] → [Decide] → [Act]│  │
│  │  POST /borrow │  │                                      │  │
│  │  GET  /books  │  │  ParseQuery → SearchBooks →          │  │
│  │  GET  /users  │  │  CheckAvailable → FilterBorrowed →   │  │
│  │  GET  /history│  │  ExecuteBorrow → ReturnResult        │  │
│  └──────┬───────┘  └──────────────────┬───────────────────┘  │
│         │                             │                       │
│         ▼                             ▼                       │
│  ┌──────────────────────────────────────────────────────┐    │
│  │               数据层 + 基础设施                        │    │
│  │  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  │    │
│  │  │ BookRepo     │  │ UserRepo     │  │ DeepSeek    │  │    │
│  │  │ (JPA/H2)     │  │ (JPA/H2)     │  │ API Client  │  │    │
│  │  └─────────────┘  └──────────────┘  └─────────────┘  │    │
│  └──────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. 技术栈

| 层级 | 选型 | 说明 |
|------|------|------|
| **前端** | Vue 3 + Element Plus + Vite | SPA 单页应用 |
| **后端框架** | Spring Boot 3.x | REST API + DI + 配置管理 |
| **OODA 引擎** | Embabel | 核心 Agent 编排 |
| **LLM** | DeepSeek API (deepseek-chat) | 自然语言解析 |
| **数据库** | H2 (开发) / MySQL (生产) | Spring Data JPA |
| **构建** | Maven (后端) + npm (前端) | 分离构建 |
| **测试** | JUnit 5 + Mockito + Vue Test Utils | 全栈测试 |

---

## 4. 领域模型

### 4.1 Java Record（核心领域对象）

```java
public record Book(
    String id,
    String title,
    String category,    // 分类：科幻、推理、计算机...
    String author,
    boolean available   // 动态状态：每次 OODA 循环实时读取
) {}

public record User(
    String id,
    String name,
    List<String> borrowedBookIds  // 动态增长
) {}

public record BorrowRequest(
    User user,
    String query                 // 自然语言，如 "我想借一些科幻小说"
) {}

public record ParsedQuery(
    String category,   // 解析结果：分类
    String keyword,    // 解析结果：关键词
    String author      // 解析结果：作者
) {}
```

### 4.2 JPA Entity（持久化对象）

```java
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    private String id;
    private String title;
    private String category;
    private String author;
    private boolean available;
    
    // 转领域对象
    public Book toDomain() { ... }
}

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private String id;
    private String name;
    
    @OneToMany(mappedBy = "user")
    private List<BorrowRecordEntity> borrowRecords;
    
    public User toDomain() { ... }
}

@Entity
@Table(name = "borrow_records")
public class BorrowRecordEntity {
    @Id
    private String id;
    @ManyToOne
    private UserEntity user;
    @ManyToOne
    private BookEntity book;
    private LocalDateTime borrowTime;
    private LocalDateTime returnTime;
}
```

---

## 5. OODA Action 设计

| Action | OODA 阶段 | Cost | 输入 → 输出 | 说明 |
|--------|-----------|------|-------------|------|
| `ParseQueryAction` | **Orient** | 5 | `BorrowRequest` → `ParsedQuery` | 调 DeepSeek API 解析自然语言 |
| `SearchBooksAction` | **Decide** | 2 | `ParsedQuery` → `List<Book>` | 按分类/关键词/作者查数据库 |
| `CheckAvailabilityAction` | **Decide** | 0 | `List<Book>` → `List<Book>` | 过滤 `available=true`，纯内存 |
| `FilterBorrowedBooksAction` | **Decide** | 0 | `List<Book>`, `User` → `List<Book>` | 过滤 `borrowedBookIds`，纯内存 |
| `ExecuteBorrowAction` | **Act** | 1 | `Book`, `User` → `BorrowResult` | 写数据库，标记 `@AchievesGoal` |
| `ReturnResultAction` | **Act** | 0 | `BorrowResult` → `String` | 格式化结果返回前端 |

**Cost 设计原则**：
- 调 DeepSeek API → cost=5（网络 IO + 模型推理）
- 查数据库 → cost=2
- 写数据库 → cost=1
- 纯内存 → cost=0

### DeepSeek 解析 Prompt 设计

```java
// ParseQueryAction 中调用 DeepSeek API
String systemPrompt = """
    你是一个图书查询解析助手。请将用户的自然语言查询解析为结构化信息。
    返回 JSON 格式，包含以下字段：
    - category: 图书分类（科幻、推理、计算机、文学、历史...），如果没有则为 null
    - keyword: 关键词，如果没有则为 null
    - author: 作者名，如果没有则为 null
    
    示例：
    用户： "我想借一些科幻小说"
    返回： {"category": "科幻", "keyword": null, "author": null}
    
    用户： "有没有东野圭吾的推理小说"
    返回： {"category": "推理", "keyword": null, "author": "东野圭吾"}
    
    用户： "推荐机器学习的书"
    返回： {"category": "计算机", "keyword": "机器学习", "author": null}
    """;
```

---

## 6. Condition 与 Goal

### Condition 守卫

| Condition | 守卫 Action | 判断逻辑 |
|-----------|-------------|---------|
| `HasValidQuery` | `ParseQueryAction` | `query != null && !isBlank()` |
| `IsAvailable` | `CheckAvailabilityAction` | `book.available() == true` —— **实时读库** |
| `NotBorrowedBefore` | `FilterBorrowedBooksAction` | `!user.borrowedBookIds().contains(book.id())` |

### Goal 终止条件

| Goal | 触发方式 | 说明 |
|------|---------|------|
| `BORROW_SUCCESS` | `@AchievesGoal("BORROW_SUCCESS")` | 成功借阅，返回借书凭证 |
| `INFORM_USER` | 内置回退 | 无书可借时告知用户并推荐替代 |

---

## 7. 完整数据流（从前端到后端）

```
┌─ 前端 ─────────────────────────────────────────────────────┐
│  用户在搜索框输入："我想借一些科幻小说"                      │
│  POST /api/borrow  { userId:"u001", query:"我想借一些科幻小说" }│
└──────────────────────────┬─────────────────────────────────┘
                           ↓
┌─ Spring Boot Controller ───────────────────────────────────┐
│  @PostMapping("/api/borrow")                                │
│  public Result borrow(@RequestBody BorrowRequest req) {     │
│      return agent.execute(req);  // 触发 OODA 循环         │
│  }                                                          │
└──────────────────────────┬─────────────────────────────────┘
                           ↓
┌─ Embabel OODA Loop ───────────────────────────────────────┐
│                                                             │
│  [Observe]  接收 BorrowRequest                              │
│                                                             │
│  [Orient]   ParseQueryAction (cost=5)                       │
│             → 调 DeepSeek API                               │
│             → 返回 ParsedQuery(category="科幻")              │
│                                                             │
│  [Decide]   SearchBooksAction (cost=2)                      │
│             → SELECT * FROM books WHERE category='科幻'     │
│             → 返回 [三体✅, 沙丘✅, 基地❌]                   │
│                                                             │
│             CheckAvailabilityAction (cost=0)                │
│             Condition: IsAvailable → 过滤"基地"             │
│             → 返回 [三体✅, 沙丘✅]                           │
│                                                             │
│             FilterBorrowedBooksAction (cost=0)              │
│             Condition: NotBorrowedBefore                    │
│             → 查用户已借列表 → 三体已借 → 过滤              │
│             → 返回 [沙丘✅]                                  │
│                                                             │
│             结果非空 → Decide 借《沙丘》                     │
│                                                             │
│  [Act]      ExecuteBorrowAction (cost=1) @AchievesGoal      │
│             → UPDATE books SET available=false WHERE id=?   │
│             → INSERT INTO borrow_records ...                │
│             → 返回 "成功为您借阅《沙丘》(弗兰克·赫伯特)"     │
│                                                             │
└──────────────────────────┬─────────────────────────────────┘
                           ↓
┌─ 前端 ─────────────────────────────────────────────────────┐
│  展示结果弹窗：✅ 成功借阅《沙丘》                           │
│  刷新：图书列表 → 沙丘状态变为"已借出"                     │
│  刷新：借阅历史 → 新增沙丘记录                             │
└────────────────────────────────────────────────────────────┘
```

---

## 8. REST API 设计

| 方法 | 路径 | 请求体 | 响应 | 说明 |
|------|------|--------|------|------|
| `POST` | `/api/borrow` | `{ "userId": "u001", "query": "我想借科幻小说" }` | `{ "success": true, "message": "...", "goal": "BORROW_SUCCESS", "book": {...} }` | **核心接口**，触发 OODA 循环 |
| `GET` | `/api/books` | — | `[{id, title, category, author, available}]` | 图书列表，前端展示用 |
| `GET` | `/api/books/search?q=` | — | `[...]` | 按关键词搜索图书 |
| `GET` | `/api/users/{id}` | — | `{id, name, borrowedBookIds}` | 用户信息 |
| `GET` | `/api/users/{id}/history` | — | `[{book, borrowTime}]` | 借阅历史 |
| `GET` | `/api/recommend?userId=` | — | `[{book, reason}]` | 智能推荐（可选扩展） |

---

## 9. 前端设计（Vue 3 + Element Plus）

### 页面布局

```
┌──────────────────────────────────────────────────────────┐
│  📚 Embabel 智能图书借阅                       用户: 张三 │
├──────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────┐│
│  │  🔍  [我想借一些科幻小说              ]  [借书]      ││
│  │      试试说："有没有东野圭吾的书" "推荐机器学习的书"  ││
│  └──────────────────────────────────────────────────────┘│
├──────────────────────┬───────────────────────────────────┤
│  图书浏览            │  借阅历史                         │
│                      │                                   │
│  ┌───┐ ┌───┐ ┌───┐  │  ✅ 三体   2026-06-01            │
│  │三体│ │沙丘│ │基地│  │  ✅ 百年孤独 2026-06-10        │
│  │已借│ │可借│ │可借│  │  📖 沙丘   刚刚借出            │
│  └───┘ └───┘ └───┘  │                                   │
│                      │                                   │
│  搜索结果显示区域      │                                   │
│  ┌──────────────────┐│                                   │
│  │ 📖 沙丘 (可借)    ││                                   │
│  │ 弗兰克·赫伯特     ││                                   │
│  │ [立即借阅]        ││                                   │
│  └──────────────────┘│                                   │
└──────────────────────┴───────────────────────────────────┘
```

### 核心交互流程

```
1. 用户在搜索框输入 → 点击"借书"
2. 前端 POST /api/borrow
3. 后端 OODA 循环执行 → 返回结果
4. 前端弹窗显示结果 → 自动刷新图书列表和历史
```

### 前端关键组件

```
src/
├── App.vue                          # 主布局
├── views/
│   ├── LibraryView.vue              # 图书借阅主页面
│   └── HistoryView.vue              # 借阅历史页面
├── components/
│   ├── SearchBox.vue                # 自然语言搜索框
│   ├── BookCard.vue                 # 图书卡片（可借/不可借状态）
│   ├── BookList.vue                 # 图书列表
│   ├── BorrowDialog.vue             # 借书结果弹窗
│   └── BorrowHistory.vue            # 借阅历史列表
├── api/
│   └── library.js                   # Axios 封装的后端 API
└── router/
    └── index.js                     # 路由配置
```

---

## 10. 项目结构（完整版）

```
embabel-book/
├── backend/                                 # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/example/library/
│       ├── LibraryApplication.java          # Spring Boot 入口
│       ├── controller/
│       │   ├── BorrowController.java        # POST /api/borrow
│       │   ├── BookController.java          # GET /api/books
│       │   └── UserController.java          # GET /api/users
│       ├── agent/
│       │   └── LibraryAgent.java            # Embabel Agent 编排
│       ├── domain/                          # 领域模型 (record)
│       │   ├── Book.java
│       │   ├── User.java
│       │   ├── BorrowRequest.java
│       │   └── ParsedQuery.java
│       ├── action/
│       │   ├── ParseQueryAction.java        # @Action(cost=5) → DeepSeek
│       │   ├── SearchBooksAction.java       # @Action(cost=2)
│       │   ├── CheckAvailabilityAction.java # @Action(cost=0)
│       │   ├── FilterBorrowedBooksAction.java # @Action(cost=0)
│       │   ├── ExecuteBorrowAction.java     # @Action(cost=1) @AchievesGoal
│       │   └── ReturnResultAction.java      # @Action(cost=0)
│       ├── condition/
│       │   ├── HasValidQueryCondition.java
│       │   ├── IsAvailableCondition.java
│       │   └── NotBorrowedBeforeCondition.java
│       ├── goal/
│       │   └── BorrowSuccessGoal.java
│       ├── repository/
│       │   ├── BookRepository.java          # Spring Data JPA
│       │   └── UserRepository.java
│       ├── entity/
│       │   ├── BookEntity.java              # JPA Entity
│       │   ├── UserEntity.java
│       │   └── BorrowRecordEntity.java
│       ├── service/
│       │   ├── BookService.java
│       │   ├── UserService.java
│       │   └── DeepSeekService.java         # DeepSeek API 调用封装
│       └── dto/
│           ├── BorrowRequestDTO.java
│           └── BorrowResultDTO.java
│
├── frontend/                                # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── App.vue
│       ├── main.js
│       ├── views/LibraryView.vue
│       ├── components/
│       │   ├── SearchBox.vue
│       │   ├── BookCard.vue
│       │   ├── BookList.vue
│       │   ├── BorrowDialog.vue
│       │   └── BorrowHistory.vue
│       └── api/library.js
│
├── ARCHITECTURE.md                          # 本文件
└── README.md                                # 项目说明
```

---

## 11. 测试策略

### 分层测试

```
        ╱╲
       ╱  E2E 测试  ╲        ← Cypress：模拟用户完整操作
      ╱  (前端→后端→DB) ╲
     ╱━━━━━━━━━━━━━━━━━━╲
    ╱  集成测试             ╲    ← Spring Boot @SpringBootTest
   ╱   (OODA 全链路)         ╲     验证 Agent 编排 + 数据库
  ╱━━━━━━━━━━━━━━━━━━━━━━━━━━╲
 ╱  Action + Condition        ╲   ← JUnit 5 + Mockito
╱   单元测试                    ╲    每个 Action 2-3 个用例
╱━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╲
```

### 后端测试

```java
// 1. 单元测试 — 测试 Condition 逻辑
@SpringBootTest
class NotBorrowedBeforeConditionTest {
    @Test
    void 用户未借过该书_返回true() {
        var user = new User("u1", "张三", List.of("book-1"));
        var book = new Book("book-2", "沙丘", "科幻", "赫伯特", true);
        assertTrue(condition.test(user, book));
    }
    
    @Test
    void 用户已借过该书_返回false() {
        var user = new User("u1", "张三", List.of("book-1"));
        var book = new Book("book-1", "三体", "科幻", "刘慈欣", true);
        assertFalse(condition.test(user, book));
    }
}

// 2. 集成测试 — 测试 OODA 全链路
@SpringBootTest
class LibraryAgentTest {
    @Autowired private LibraryAgent agent;
    @Autowired private BookRepository bookRepo;
    @Autowired private UserRepository userRepo;
    
    @Test
    void 成功借阅() {
        // 准备：插入测试数据
        bookRepo.save(new BookEntity("b1", "沙丘", "科幻", "赫伯特", true));
        userRepo.save(new UserEntity("u1", "张三"));
        
        // 执行 OODA 循环
        var request = new BorrowRequest("u1", "我想借科幻小说");
        var result = agent.execute(request);
        
        // 验证
        assertEquals("BORROW_SUCCESS", result.goal());
        assertTrue(result.message().contains("沙丘"));
        assertFalse(bookRepo.findById("b1").get().isAvailable());
    }
    
    @Test
    void 所有书都不可借_回退INFORM_USER() {
        bookRepo.save(new BookEntity("b1", "沙丘", "科幻", "赫伯特", false));
        var request = new BorrowRequest("u1", "我想借科幻小说");
        var result = agent.execute(request);
        assertEquals("INFORM_USER", result.goal());
    }
    
    @Test
    void DeepSeek解析提示词测试_验证JSON返回格式() {
        var deepseek = new DeepSeekService(mockRestTemplate);
        when(restTemplate.postForObject(...)).thenReturn(...);
        var result = deepseek.parse("有没有东野圭吾的书");
        assertEquals("东野圭吾", result.getAuthor());
    }
}

// 3. Controller 测试
@WebMvcTest(BorrowController.class)
class BorrowControllerTest {
    @Test
    void POST借书_返回正确格式() throws Exception {
        mockMvc.perform(post("/api/borrow")
                .contentType(JSON)
                .content("{\"userId\":\"u1\",\"query\":\"借科幻\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.goal").value("BORROW_SUCCESS"));
    }
}
```

### 前端测试

```javascript
// Vue 组件测试 (Vitest)
describe('SearchBox.vue', () => {
    it('用户输入后点击按钮触发借书', async () => {
        const wrapper = mount(SearchBox);
        const input = wrapper.find('input');
        await input.setValue('我想借科幻小说');
        await wrapper.find('button').trigger('click');
        expect(wrapper.emitted('borrow')[0][0].query).toBe('我想借科幻小说');
    });
    
    it('搜索建议展示', () => {
        const wrapper = mount(SearchBox);
        expect(wrapper.text()).toContain('试试说');
    });
});

// E2E 测试 (Cypress)
describe('完整借书流程', () => {
    it('用户从输入到看到结果', () => {
        cy.visit('/library');
        cy.get('input').type('我想借科幻小说');
        cy.get('button').contains('借书').click();
        cy.get('.el-dialog').should('be.visible');
        cy.contains('成功');
    });
});
```

---

## 12. 环境配置

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:library
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

deepseek:
  api-key: ${DEEPSEEK_API_KEY}
  model: deepseek-chat
  temperature: 0.1  # 解析查询需要低温度，保证一致性
  base-url: https://api.deepseek.com/v1

server:
  port: 8080
```

### 前端环境变量 (.env)

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

---

## 13. 运行方式

```bash
# 1. 启动后端（H2 内存数据库 + 预置测试数据）
cd backend
mvn spring-boot:run

# 2. 启动前端（开发模式）
cd frontend
npm install
npm run dev

# 3. 浏览器访问
open http://localhost:5173

# 4. 在搜索框输入：
#    "我想借一些科幻小说"
#    "有没有东野圭吾的书"
#    "推荐机器学习的入门书"
```

---

## 14. 设计要点总结

| 问题 | 方案 |
|------|------|
| **LLM 用哪个？** | DeepSeek API（deepseek-chat），低温度确保解析一致性 |
| **后端架构？** | Spring Boot 3.x，提供 REST API、JPA 持久化、DI 管理 |
| **前端要吗？** | Vue 3 + Element Plus，搜索框 + 图书展示 + 结果弹窗 |
| **数据放哪？** | 开发期 H2 内存库，一行命令启动即用 |
| **怎么体现 OODA？** | 每次 `POST /api/borrow` 触发完整 OODA 循环，实时查库动态感知 |
| **怎么测试？** | 单元测试（Condition/Action）+ 集成测试（OODA 全链路）+ E2E（Cypress） |
