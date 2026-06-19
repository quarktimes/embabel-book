# Embabel 智能图书借阅 Agent — 开发计划

## 开发分期总览

```
总工期：约 5-7 天（每天 2-4 小时）

Phase 1: 项目骨架  (Day 1)    → 能跑起来
Phase 2: 核心逻辑  (Day 2-3)  → OODA 循环通
Phase 3: 界面交互  (Day 4)    → 能在页面上借书
Phase 4: 测试完善  (Day 5)    → 覆盖主要场景
Phase 5: 演示准备  (Day 6)    → 预置数据 + README
```

---

## Phase 1：项目骨架（Day 1，约 3h）

### Step 1.1 — 初始化 Spring Boot 项目（30min）

```bash
# 用 Spring Initializr 生成项目骨架
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.4.0 \
  -d baseDir=backend \
  -d groupId=com.example \
  -d artifactId=library \
  -d packageName=com.example.library \
  -d dependencies=web,thymeleaf,data-jpa,h2,validation,lombok \
  -o backend.zip

# 解压到  目录
unzip backend.zip -d 

# 验证能启动
cd . && mvn spring-boot:run
```

**产出物**：`pom.xml`，Spring Boot 空项目能启动

### Step 1.2 — 初始化前端资源目录（30min）

创建模板和静态资源目录结构：

```
src/main/resources/
├── templates/
│   ├── index.html            # 主页面
│   └── fragments/
│       ├── bookList.html      # 图书列表片段
│       ├── borrowResult.html  # 借书结果片段
│       └── history.html       # 借阅历史片段
├── static/
│   └── css/
│       └── style.css          # 自定义样式
└── application.yml            # 配置文件
```

### Step 1.3 — 配置 application.yml（30min）

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:library
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  thymeleaf:
    cache: false

deepseek:
  api-key: ${DEEPSEEK_API_KEY:sk-demo-key}
  model: deepseek-chat
  temperature: 0.1
  base-url: https://api.deepseek.com/v1

server:
  port: 8080
```

### Step 1.4 — 创建完整的 pom.xml（30min）

```xml
<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- Thymeleaf 模板引擎 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <!-- JPA + H2 数据库 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <!-- Embabel 框架 -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>embabel-core</artifactId>
        <version>1.0.0</version>
    </dependency>
    <!-- DeepSeek API -->
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-ai-openai</artifactId>
        <version>1.0.0</version>
    </dependency>
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### ✅ Phase 1 验收标准

```
mvn spring-boot:run 能正常启动，无报错
浏览器访问 http://localhost:8080 能看到空白页面
```

---

## Phase 2：核心逻辑 — 契约先行（Day 2-4，约 10h）

### 契约先行的工作方式

每个 Step 按三步走：

```
Step N: Xxx
 ① 定契约 → 列出这个类的输入/输出/边界场景/异常情况
 ② 写测试 → 针对契约写测试用例（含错误路径和边界）
 ③ 写实现 → 让测试变绿
       ↓  mvn test 全绿才进入下一步
```

**关键原则**：测试不关心内部实现，只关心契约是否满足。契约定义的是"该干什么"，不是"代码干了什么"。

---

### Step 2.1 — BookEntity + 测试（30min）

**依赖**：无（只依赖 `Book` record，已存在）

**契约**：
```
BookEntity  ↔  books 表
- id 为主键
- title/category/author/available 均有映射
- toDomain() 将 Entity 转成 Book record
- fromDomain(Book) 将 Book 转成 Entity
```

**测试需覆盖**：
```java
// 正向
@Test void toDomain_字段映射正确()
@Test void fromDomain_字段映射正确()
@Test void 保存后从数据库读取_字段一致()

// 边界
@Test void 长书名字段不超长()
@Test void 分类名为空时能保存()  // 特殊场景
```

**产出**：`entity/BookEntity.java` + `entity/BookEntityTest.java`

---

### Step 2.2 — UserEntity + BorrowRecordEntity + 测试（30min）

**依赖**：BookEntity

**契约**：
```
UserEntity  ↔  users 表
- id 为主键，name 为姓名
- borrowRecords 一对多关联借书记录
- toDomain() 转 User record，只包含未归还的图书 ID

BorrowRecordEntity  ↔  borrow_records 表
- id 自增主键
- user 多对一关联 UserEntity
- book 多对一关联 BookEntity
- borrowTime 借书时间
- returnTime 还书时间（null 表示未还）
```

**测试需覆盖**：
```java
@Test void User_toDomain_包含未归还的借书记录()
@Test void User_toDomain_不包含已归还的记录()
@Test void User_toDomain_无借书记录时返回空列表()
@Test void BorrowRecordEntity_关联关系正确()
```

**产出**：`entity/UserEntity.java` + `entity/BorrowRecordEntity.java` + 对应测试

---

### Step 2.3 — Repository + 测试（20min）

**依赖**：BookEntity, UserEntity, BorrowRecordEntity

**契约**：
```
BookRepository:
  findByCategoryContaining(category) → List<BookEntity>
  findByAuthorContaining(author)     → List<BookEntity>
  findByTitleContaining(keyword)     → List<BookEntity>

UserRepository: 标准 CRUD

BorrowRecordRepository:
  findByUserIdOrderByBorrowTimeDesc(userId) → List<BorrowRecordEntity>
```

**测试需覆盖**：
```java
// 正向
@Test void 按分类查询_返回匹配图书()
@Test void 分类名大小写不敏感()         // "科幻" 和 "KEHUAN"
@Test void 分类名部分匹配()              // "科" 也能查到

// 边界
@Test void 无匹配分类_返回空列表()
@Test void 多条件查询_优先使用category()  // 理解优先级的契约
```

**产出**：3 个 Repository 接口 + `RepositoryTest.java`

---

### Step 2.4 — HasValidQueryCondition + 测试（15min）

**依赖**：无（纯逻辑）

**契约**：
```
HasValidQueryCondition.test(BorrowRequest)
- query == null      → false
- query == ""        → false
- query == "   "     → false（纯空格）
- query == "科幻"    → true
```

**测试覆盖**：全部 4 种情况

**产出**：`condition/HasValidQueryCondition.java`

---

### Step 2.5 — IsAvailableCondition + 测试（15min）

**依赖**：BookRepository（读库实时检查）

**契约**：
```
IsAvailableCondition.test(Book)
- 数据库里这本书 available = true  → true
- 数据库里这本书 available = false → false
- 数据库里没有这本书               → false（找不到按不可借处理）
- 每次调用重新读库，不缓存          → 两次调用结果可以不同
```

**测试覆盖**：4 种情况（true/false/不存在/两次调用不同结果）

**产出**：`condition/IsAvailableCondition.java`

---

### Step 2.6 — NotBorrowedBeforeCondition + 测试（15min）

**依赖**：User（纯内存比较，只查 User.borrowedBookIds）

**契约**：
```
NotBorrowedBeforeCondition.test(User, Book)
- User.borrowedBookIds 不包含 Book.id → true
- User.borrowedBookIds 包含 Book.id   → false
- User.borrowedBookIds 为空列表       → true
- User.borrowedBookIds 为 null        → true（防御性编程）
```

**测试覆盖**：4 种情况

**产出**：`condition/NotBorrowedBeforeCondition.java`

---

### Step 2.7 — DeepSeekService + 降级 + 测试（45min）

**依赖**：无（独立的外部 API 封装）

**契约**：
```
DeepSeekService.parseQuery(String userQuery) → ParsedQuery
- 正常情况：调 DeepSeek API，返回 ParsedQuery
- API 不可用：降级为关键词匹配
- 降级策略："我想借科幻小说" → ParsedQuery(category="科幻")
- 降级策略："东野圭吾推理"  → ParsedQuery(author="东野圭吾", category="推理")
- API key 为空：直接走降级，不报错
```

**Prompt Prompt 设计（定义好就不动）**：
```
系统："你是一个图书查询解析助手。返回 JSON：{category, keyword, author}"
样例1：用户"我想借科幻小说" → {"category":"科幻","keyword":null,"author":null}
样例2：用户"东野圭吾的推理小说" → {"category":"推理","keyword":null,"author":"东野圭吾"}
```

**测试覆盖**：
```java
@Test void 解析科幻查询_返回正确JSON()
@Test void 解析作者加分类_返回正确JSON()
@Test void 解析关键词查询_返回正确JSON()
@Test void API调用失败_降级为关键词匹配()
@Test void APIKey为空_走降级不报错()
@Test void 降级逻辑_中文分词_科幻想科幻小说()  // 边界
```

**产出**：`service/DeepSeekService.java`

---

### Step 2.8 — ParseQueryAction + 测试（20min）

**依赖**：DeepSeekService, HasValidQueryCondition

**契约**：
```
ParseQueryAction.execute(BorrowRequest) → ParsedQuery
- 请求有效 → 调 DeepSeekService.parseQuery
- 请求无效（空查询）→ 由 Condition 拦截不进入此 Action
- 执行后触发 info 日志：action=ParseQuery cost=5
```

**测试覆盖**：
```java
@Test void 有效请求_返回解析结果()
@Test void 请求为空_Condition拦截_action不被调用()
```

**产出**：`action/ParseQueryAction.java`

---

### Step 2.9 — SearchBooksAction + 测试（20min）

**依赖**：BookRepository

**契约**：
```
SearchBooksAction.execute(ParsedQuery) → List<Book>
- category 优先：parsed.category != null → 按分类搜索
- author 次之：parsed.author != null → 按作者搜索
- keyword 最后：parsed.keyword != null → 按关键词搜索
- 三个都 null → 返回空列表
- 搜索结果转 domain 对象返回
```

**测试覆盖**：
```java
@Test void 按分类搜索_返回正确图书()
@Test void 按作者搜索_返回正确图书()
@Test void 按关键词搜索_返回正确图书()
@Test void 分类优先_三个参数都有时用category()
@Test void 三个参数都null_返回空()
@Test void 无匹配分类_返回空()
```

**产出**：`action/SearchBooksAction.java`

---

### Step 2.10 — CheckAvailabilityAction + 测试（20min）

**依赖**：IsAvailableCondition

**契约**：
```
CheckAvailabilityAction.execute(List<Book>) → List<Book>
- 输入列表中的 Book，逐本检查 IsAvailableCondition
- 过滤掉不可借的，保留可借的
- 全部不可借 → 返回空列表
- 空输入 → 返回空列表
```

**测试覆盖**：
```java
@Test void 过滤不可借_保留可借()
@Test void 全部不可借_返回空()
@Test void 全部可借_返回所有()
@Test void 空输入_返回空()
@Test void 条件动态性_第一次可借_第二次不可借()
```

**产出**：`action/CheckAvailabilityAction.java`

---

### Step 2.11 — FilterBorrowedBooksAction + 测试（20min）

**依赖**：NotBorrowedBeforeCondition

**契约**：
```
FilterBorrowedBooksAction.execute(List<Book>, User) → List<Book>
- 过滤掉 User 已借过的书
- 全部已借过 → 返回空列表
- 全部未借过 → 返回所有
- 空输入 → 返回空列表
```

**测试覆盖**：
```java
@Test void 过滤已借_保留未借()
@Test void 全部已借_返回空()
@Test void 全部未借_返回所有()
@Test void 部分已借_部分未借()
@Test void 空输入_返回空()
```

**产出**：`action/FilterBorrowedBooksAction.java`

---

### Step 2.12 — ExecuteBorrowAction + 测试（30min）

**依赖**：BookRepository, UserRepository, BorrowRecordRepository

**契约**：
```
ExecuteBorrowAction.execute(Book, User) → BorrowResult
- 前置校验：二次确认 Book.available == true
- 不满足 → 抛 BookNotAvailableException
- 更新 book.available = false
- 创建 BorrowRecord（borrowTime = now, returnTime = null）
- 返回 BorrowResult(success=true, message包含书名)
- 操作是原子的（事务性）
```

**测试覆盖**：
```java
@Test void 借书成功_图书状态变false()
@Test void 借书成功_新增一条借书记录()
@Test void 借书成功_返回成功信息()
@Test void 图书已被借走_抛异常()
@Test void 同一本书不能重复借()
@Test void 事务回滚_出错时数据不变()
```

**产出**：`action/ExecuteBorrowAction.java`

---

### Step 2.13 — ReturnResultAction + 测试（10min）

**依赖**：无

**契约**：
```
ReturnResultAction.execute(BorrowResult) → String
- result.success == true  → 返回 result.message
- result.success == false → 返回"抱歉，暂时无法借阅"
- message 包含书名信息
```

**测试覆盖**：
```java
@Test void 成功时返回信息()
@Test void 失败时返回默认提示()
```

**产出**：`action/ReturnResultAction.java`

---

### Step 2.14 — LibraryAgent + 集成测试（45min）

**依赖**：所有 6 个 Action + 3 个 Condition

**契约**：
```
LibraryAgent.execute(BorrowRequest) → String
- 空查询 → 引导用户输入
- 无匹配图书 → 告知无符合条件图书
- 全部不可借 → 告知已出借
- 全部已借过 → 告知已借过
- 有可借未借的 → 借第一本，返回成功信息
- 集成验证：借书后数据库状态变更
```

**集成测试覆盖**：
```java
@SpringBootTest @AutoConfigureMockMvc
@Test void 全链路_成功借阅()
@Test void 空查询_返回引导提示()
@Test void 无匹配分类_返回提示()
@Test void 全部不可借_返回提示()
@Test void 全部已借过_返回提示()
@Test void 多次借书_动态状态变化()
@Test void 不同用户互不影响()
```

**产出**：`agent/LibraryAgent.java` + `LibraryAgentIntegrationTest.java`

---

### Step 2.15 — DemoDataLoader（15min）

**依赖**：BookEntity, UserEntity, BorrowRecordEntity, 所有 Repository

**产出**：`config/DemoDataLoader.java`

预置 10 本书 + 2 个用户 + 张三已借 2 本的演示数据。

---

### ✅ Phase 2 验收标准

```
mvn test 全部通过
每个 Step 的契约场景全覆盖
  ✅ Step 2.1  BookEntity 映射正确
  ✅ Step 2.2  UserEntity/BorrowRecordEntity 关联正确
  ✅ Step 2.3  Repository 查询正确（含大小写容错）
  ✅ Step 2.4  HasValidQueryCondition 空值/空格拦截
  ✅ Step 2.5  IsAvailableCondition 实时读库
  ✅ Step 2.6  NotBorrowedBeforeCondition 已借过滤
  ✅ Step 2.7  DeepSeekService 正常/降级双路径
  ✅ Step 2.8  ParseQueryAction 调用 LLM
  ✅ Step 2.9  SearchBooksAction 分类/作者/关键词
  ✅ Step 2.10 CheckAvailabilityAction 过滤不可借
  ✅ Step 2.11 FilterBorrowedBooksAction 过滤已借
  ✅ Step 2.12 ExecuteBorrowAction 事务性借书
  ✅ Step 2.13 ReturnResultAction 格式化结果
  ✅ Step 2.14 LibraryAgent 全链路 OODA 编排
  ✅ Step 2.15 DemoDataLoader 演示数据就绪
```

---

## Phase 3：界面交互（Day 4，约 3h）

### Step 3.1 — 实现 Controller（30min）

```java
// controller/LibraryController.java
@Controller
public class LibraryController {
    
    @Autowired private LibraryAgent agent;
    @Autowired private BookRepository bookRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private BorrowRecordRepository borrowRecordRepo;
    
    // 主页面
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("books", bookRepo.findAll().stream()
            .map(BookEntity::toDomain).toList());
        model.addAttribute("users", userRepo.findAll());
        return "index";
    }
    
    // 借书（返回 HTML 片段，htmx 局部刷新）
    @PostMapping("/borrow")
    public String borrow(@RequestParam String userId,
                         @RequestParam String query,
                         Model model) {
        var request = new BorrowRequest(userId, query);
        String message = agent.execute(request);
        
        model.addAttribute("message", message);
        model.addAttribute("books", bookRepo.findAll().stream()
            .map(BookEntity::toDomain).toList());
        model.addAttribute("borrowRecords", 
            borrowRecordRepo.findByUserIdOrderByBorrowTimeDesc(userId));
        return "fragments :: borrowResult";
    }
    
    // 刷新图书列表
    @GetMapping("/books")
    public String books(Model model) {
        model.addAttribute("books", bookRepo.findAll().stream()
            .map(BookEntity::toDomain).toList());
        return "fragments :: bookList";
    }
}
```

### Step 3.2 — 实现主页面模板（1h）

```html
<!-- templates/index.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Embabel 智能图书借阅</title>
    <script src="https://unpkg.com/htmx.org@1.9.12"></script>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="/css/style.css" rel="stylesheet">
</head>
<body>
    <div class="container py-4">
        <!-- 顶部导航 -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1>📚 Embabel 智能图书借阅</h1>
            <div th:if="${users}">
                <select id="userSelect" class="form-select" style="width: auto;">
                    <option th:each="u : ${users}" th:value="${u.id}" th:text="${u.name}"></option>
                </select>
            </div>
        </div>
        
        <!-- 搜索区 -->
        <div class="card mb-4">
            <div class="card-body">
                <div class="input-group input-group-lg">
                    <input type="text" id="query" class="form-control"
                           placeholder="我想借一些科幻小说"
                           autofocus>
                    <button class="btn btn-primary"
                            hx-post="/borrow"
                            hx-target="#result-area"
                            hx-include="#userSelect, #query"
                            hx-swap="innerHTML">
                        📚 借书
                    </button>
                </div>
                <div class="mt-2 text-muted small">
                    💡 试试说：「有没有东野圭吾的书」「推荐机器学习的入门书」「我想借推理小说」
                </div>
            </div>
        </div>
        
        <!-- 结果弹窗区域 -->
        <div id="result-area"></div>
        
        <!-- 图书列表 -->
        <div class="row" th:replace="fragments :: bookList"></div>
    </div>
</body>
</html>
```

### Step 3.3 — 实现片段模板（1h）

```html
<!-- templates/fragments/borrowResult.html -->
<div th:fragment="borrowResult" 
     th:classappend="${message.contains('成功')} ? 'alert alert-success' : 'alert alert-info'"
     hx-swap-oob="true" id="result-area">
    <div class="d-flex justify-content-between align-items-center">
        <span th:text="${message}">结果</span>
        <button type="button" class="btn-close" 
                onclick="this.closest('.alert').remove()"></button>
    </div>
</div>

<!-- 替换图书列表（htmx 支持多个 target 同时替换） -->
<div th:fragment="bookList" id="book-list" hx-swap-oob="true">
    <th:block th:each="book : ${books}">
        <div class="col-md-3 mb-3">
            <div class="card h-100"
                 th:classappend="${book.available()} ? 'border-success' : 'border-secondary'">
                <div class="card-body">
                    <h5 class="card-title" th:text="${book.title()}">书名</h5>
                    <p class="card-text text-muted small" th:text="${book.author()}">作者</p>
                    <span class="badge bg-light text-dark" th:text="${book.category()}">分类</span>
                    <span class="badge" 
                          th:classappend="${book.available()} ? 'bg-success' : 'bg-secondary'"
                          th:text="${book.available()} ? '✅ 可借' : '❌ 已借出'">
                    </span>
                </div>
            </div>
        </div>
    </th:block>
</div>

<!-- 借阅历史 -->
<div th:fragment="history" id="history-list" hx-swap-oob="true">
    <h5>📖 借阅记录</h5>
    <ul class="list-group">
        <li class="list-group-item d-flex justify-content-between align-items-center"
            th:each="record : ${borrowRecords}">
            <span th:text="${record.book.title}">书名</span>
            <small class="text-muted" 
                   th:text="${#temporals.format(record.borrowTime, 'MM-dd HH:mm')}">时间</small>
        </li>
        <li class="list-group-item text-muted text-center" 
            th:if="${#lists.isEmpty(borrowRecords)}">
            暂无借阅记录
        </li>
    </ul>
</div>
```

### Step 3.4 — 自定义样式（30min）

```css
/* static/css/style.css */
body {
    background: #f5f5f5;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto;
}

.card {
    transition: all 0.2s ease;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}

.border-success { border-left: 4px solid #28a745; }
.border-secondary { border-left: 4px solid #6c757d; }

.alert {
    animation: slideIn 0.3s ease;
    border-radius: 12px;
}

@keyframes slideIn {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
}

#query {
    border-radius: 12px;
    border: 2px solid #e0e0e0;
}

#query:focus {
    border-color: #0d6efd;
    box-shadow: 0 0 0 3px rgba(13,110,253,0.15);
}
```

### ✅ Phase 3 验收标准

```
浏览器打开 http://localhost:8080
  ✅ 看到搜索框 + 图书列表
  ✅ 输入「我想借科幻小说」→ 点借书 → 看到成功弹窗
  ✅ 图书列表自动刷新 → 借的那本显示「已借出」
  ✅ 换个用户 → 已借图书不影响
```

---

## Phase 4：测试完善（Day 5，约 3h）

### Step 4.1 — Condition 单元测试（30min）

```java
@SpringBootTest
class ConditionTest {
    
    @Test
    void hasValidQuery_空字符串_返回false() {
        var condition = new HasValidQueryCondition();
        assertFalse(condition.test(new BorrowRequest("u1", "")));
        assertFalse(condition.test(new BorrowRequest("u1", null)));
        assertTrue(condition.test(new BorrowRequest("u1", "借科幻")));
    }
    
    @Test
    void isAvailable_可借_返回true() {
        var condition = new IsAvailableCondition();
        var book = new Book("b1", "三体", "科幻", "刘慈欣", true);
        assertTrue(condition.test(book));
    }
    
    @Test
    void notBorrowedBefore_已借过_返回false() {
        var condition = new NotBorrowedBeforeCondition();
        var user = new User("u1", "张三", List.of("b1", "b2"));
        var book = new Book("b1", "三体", "科幻", "刘慈欣", true);
        assertFalse(condition.test(user, book));
    }
}
```

### Step 4.2 — Action 单元测试（45min）

```java
@SpringBootTest
class SearchBooksActionTest {
    
    @MockBean private BookRepository bookRepository;
    @Autowired private SearchBooksAction action;
    
    @Test
    void 按分类搜索_返回正确结果() {
        when(bookRepository.findByCategoryContaining("科幻"))
            .thenReturn(List.of(
                new BookEntity("b1", "三体", "科幻", "刘慈欣", true),
                new BookEntity("b2", "沙丘", "科幻", "赫伯特", true)
            ));
        
        var result = action.execute(new ParsedQuery("科幻", null, null));
        
        assertEquals(2, result.size());
        verify(bookRepository).findByCategoryContaining("科幻");
    }
    
    @Test
    void 按作者搜索_返回正确结果() {
        when(bookRepository.findByAuthorContaining("东野圭吾"))
            .thenReturn(List.of(
                new BookEntity("b3", "白夜行", "推理", "东野圭吾", true)
            ));
        
        var result = action.execute(new ParsedQuery(null, null, "东野圭吾"));
        
        assertEquals(1, result.size());
        assertEquals("白夜行", result.get(0).title());
    }
}
```

### Step 4.3 — Agent 集成测试（1h）

```java
@SpringBootTest
@AutoConfigureMockMvc
class LibraryAgentIntegrationTest {
    
    @Autowired private MockMvc mockMvc;
    @Autowired private BookRepository bookRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private BorrowRecordRepository borrowRecordRepo;
    
    @BeforeEach
    void setUp() {
        // 预置图书
        bookRepo.save(new BookEntity("b1", "三体", "科幻", "刘慈欣", true));
        bookRepo.save(new BookEntity("b2", "沙丘", "科幻", "赫伯特", true));
        bookRepo.save(new BookEntity("b3", "白夜行", "推理", "东野圭吾", true));
        bookRepo.save(new BookEntity("b4", "嫌疑人X的献身", "推理", "东野圭吾", false));
        
        // 预置用户
        var user = new UserEntity();
        user.setId("u1");
        user.setName("张三");
        userRepo.save(user);
    }
    
    @Test
    void 全链路_成功借阅() throws Exception {
        mockMvc.perform(post("/borrow")
                .param("userId", "u1")
                .param("query", "我想借科幻小说"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("成功")))
            .andExpect(content().string(containsString("沙丘")));
        
        // 验证数据库状态
        assertFalse(bookRepo.findById("b2").get().isAvailable());
        assertEquals(1, borrowRecordRepo.count());
    }
    
    @Test
    void 全部已借过_返回提示() throws Exception {
        // 先借走所有科幻书
        mockMvc.perform(post("/borrow")
            .param("userId", "u1")
            .param("query", "我想借科幻小说"));
        mockMvc.perform(post("/borrow")
            .param("userId", "u1")
            .param("query", "再借科幻小说"));
        
        // 第三次应该提示没有了
        mockMvc.perform(post("/borrow")
                .param("userId", "u1")
                .param("query", "还要借科幻"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("已经借过")));
    }
    
    @Test
    void 空查询_返回引导提示() throws Exception {
        mockMvc.perform(post("/borrow")
                .param("userId", "u1")
                .param("query", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("请描述")));
    }
    
    @Test
    void 没有匹配的书_返回提示() throws Exception {
        mockMvc.perform(post("/borrow")
                .param("userId", "u1")
                .param("query", "我想借经济学"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("没有找到")));
    }
}
```

### Step 4.4 — DeepSeek 解析测试（30min）

```java
@SpringBootTest
class DeepSeekServiceTest {
    
    @MockBean private RestTemplate restTemplate;
    @Autowired private DeepSeekService deepSeek;
    
    @Test
    void 解析科幻查询() {
        mockDeepSeekResponse("{\"category\":\"科幻\",\"keyword\":null,\"author\":null}");
        
        var result = deepSeek.parseQuery("我想借一些科幻小说");
        
        assertEquals("科幻", result.category());
        assertNull(result.keyword());
        assertNull(result.author());
    }
    
    @Test
    void 解析作者加分类查询() {
        mockDeepSeekResponse("{\"category\":\"推理\",\"keyword\":null,\"author\":\"东野圭吾\"}");
        
        var result = deepSeek.parseQuery("有没有东野圭吾的推理小说");
        
        assertEquals("推理", result.category());
        assertEquals("东野圭吾", result.author());
    }
    
    @Test
    void 解析关键词查询() {
        mockDeepSeekResponse("{\"category\":\"计算机\",\"keyword\":\"机器学习\",\"author\":null}");
        
        var result = deepSeek.parseQuery("推荐机器学习的书");
        
        assertEquals("计算机", result.category());
        assertEquals("机器学习", result.keyword());
    }
    
    private void mockDeepSeekResponse(String json) {
        when(restTemplate.postForObject(any(), any(), eq(String.class)))
            .thenReturn(json);
    }
}
```

### ✅ Phase 4 验收标准

```
mvn test 全部通过
  ✅ 3 个 Condition 测试
  ✅ 6 个 Action 测试
  ✅ 4 个 Agent 集成测试（成功借阅 / 已借过 / 空查询 / 无匹配）
  ✅ 3 个 DeepSeek 解析测试
  覆盖率 > 80%（领域逻辑层）
```

---

## Phase 5：演示准备（Day 6，约 2h）

### Step 5.1 — 预置演示数据（30min）

```java
// config/DemoDataLoader.java
@Component
public class DemoDataLoader implements CommandLineRunner {
    
    @Override
    public void run(String... args) {
        // 10 本测试图书
        var books = List.of(
            createBook("b1", "三体", "科幻", "刘慈欣", true),
            createBook("b2", "沙丘", "科幻", "弗兰克·赫伯特", true),
            createBook("b3", "银河帝国：基地", "科幻", "阿西莫夫", true),
            createBook("b4", "白夜行", "推理", "东野圭吾", true),
            createBook("b5", "嫌疑人X的献身", "推理", "东野圭吾", true),
            createBook("b6", "百年孤独", "文学", "马尔克斯", true),
            createBook("b7", "机器学习实战", "计算机", "Peter Harrington", true),
            createBook("b8", "深度学习入门", "计算机", "斋藤康毅", true),
            createBook("b9", "统计学习方法", "计算机", "李航", false),
            createBook("b10", "人类简史", "历史", "尤瓦尔·赫拉利", true)
        );
        bookRepo.saveAll(books);
        
        // 2 个测试用户
        var user1 = new UserEntity("u1", "张三");
        var user2 = new UserEntity("u2", "李四");
        userRepo.save(user1);
        userRepo.save(user2);
        
        // 张三已借《三体》《百年孤独》
        borrowRecordRepo.save(new BorrowRecordEntity(null, user1, books.get(0), now(), null));
        borrowRecordRepo.save(new BorrowRecordEntity(null, user1, books.get(5), now(), null));
        books.get(0).setAvailable(false);
        books.get(5).setAvailable(false);
    }
}
```

### Step 5.2 — 编写 README（30min）

```markdown
# Embabel 智能图书借阅 Agent

基于 Embabel OODA 循环的智能图书借阅演示系统。

## 快速开始

```bash
# 设置 DeepSeek API Key
export DEEPSEEK_API_KEY=sk-your-key-here

# 启动
mvn spring-boot:run

# 打开浏览器
open http://localhost:8080
```

## 演示场景

| 输入 | 预期结果 | 展示点 |
|------|---------|--------|
| "我想借科幻小说" | 成功借阅《沙丘》（三体已借过，自动过滤） | OODA 动态过滤 |
| "有没有东野圭吾的书" | 成功借阅《白夜行》或《嫌疑人》 | DeepSeek 作者解析 |
| "推荐机器学习的入门书" | 成功借阅《深度学习入门》 | 关键词解析 |
| "我想借经济学" | 提示"没有找到" | 条件回退 |
| 连续借两次科幻 | 第二次提示"已经借过" | 动态历史累积 |
```

### Step 5.3 — 录制演示视频（可选，30min）

```
演示流程：
1. 启动项目 → 展示页面
2. 输入「我想借科幻小说」→ 看到沙丘被借出
3. 输入「再借科幻」→ 三体已借 + 沙丘刚借 → 提示无书
4. 切换到李四 → 输入「借科幻」→ 三体仍在可借列表
5. 输入「有没有东野圭吾的书」→ 展示作者解析
6. 查看 H2 Console → 展示数据库变化
```

### Step 5.4 — 项目清点（30min）

确认最终文件结构：

```
embabel-book/
├── 
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/example/library/
│       │   │   ├── LibraryApplication.java
│       │   │   ├── agent/LibraryAgent.java
│       │   │   ├── controller/LibraryController.java
│       │   │   ├── domain/        (5 files)
│       │   │   ├── entity/        (3 files)
│       │   │   ├── repository/    (3 files)
│       │   │   ├── service/DeepSeekService.java
│       │   │   ├── action/        (6 files)
│       │   │   ├── condition/     (3 files)
│       │   │   └── config/DemoDataLoader.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── templates/
│       │       │   ├── index.html
│       │       │   └── fragments/
│       │       │       ├── bookList.html
│       │       │       └── borrowResult.html
│       │       └── static/css/style.css
│       └── test/java/com/example/library/
│           ├── LibraryAgentIntegrationTest.java
│           ├── ConditionTest.java
│           ├── SearchBooksActionTest.java
│           └── DeepSeekServiceTest.java
├── ARCHITECTURE.md
├── DEVELOPMENT_PLAN.md
└── README.md
```

### ✅ Phase 5 验收标准

```
✅ mvn clean test 全部通过
✅ mvn spring-boot:run 正常启动
✅ 浏览器打开能看到 10 本书、2 个用户
✅ 5 个演示场景全部走通
✅ README 完整，别人能按步骤启动
```

---

## 开发总览

```
Phase 1: 项目骨架       Day 1  (3h)   ═══ 能启动 ═══
Phase 2: 核心逻辑       Day 2-4 (10h)  ═══ 契约+实现+测试 ═══
   Step 2.1-2.3   Entity+Repo    1h
   Step 2.4-2.6   Condition      45min
   Step 2.7       DeepSeek       45min
   Step 2.8-2.13  Action         2h
   Step 2.14      Agent+集成测试  45min
   Step 2.15      DemoData       15min
Phase 3: 界面交互       Day 5  (3h)   ═══ 可视化 ═══
Phase 4: 测试完善       Day 6  (1h)   ═══ 补漏 ═══
Phase 5: 演示准备       Day 6  (2h)   ═══ 可演示 ═══

总计：约 19h 开发时间，6 天日历时间
Phase 2 从原来 6h 细化为 10h = 15 个 Step，每个 10-45min
```

---

## 关键风险和缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| DeepSeek API key 未配置 | 解析查询不可用 | Phase 1 配置 fallback：无 key 时走关键词匹配降级 |
| Embabel 框架未成熟 | OODA 编排不可用 | 先用纯 Java 手动编排（LibraryAgent 已实现），后期再接入 Embabel 注解 |
| htmx 不熟悉 | 前端开发慢 | 只用最基本功能：hx-post / hx-target / hx-swap-oob，参考[官方文档](https://htmx.org/) |
| 时间不够 | 无法按时交付 | Phase 4 和 5 可并行；测试优先保证集成测试 |
