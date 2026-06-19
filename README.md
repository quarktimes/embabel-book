# 📚 Embabel 智能图书借阅 Agent

基于 **Embabel OODA 循环** 驱动的智能图书借阅 Agent。用户通过自然语言输入，Agent 自主完成意图理解 → 图书搜索 → 可用性校验 → 防重复过滤 → 借书执行的全链路。

## OODA 循环

```
[Observe]  接收用户输入
    ↓
[Orient]   DeepSeek 解析自然语言 → 结构化查询
    ↓
[Decide]   搜索图书 → 检查可借 → 过滤已借
    ↓
[Act]      执行借书 → 返回结果
    ↓
  (环境已更新，下次循环自动感知)
```

## 技术栈

| 层级 | 选型 |
|------|------|
| 后端框架 | Spring Boot 3.4 + Java 21 |
| 模板引擎 | Thymeleaf + htmx（非前后端分离） |
| 数据库 | H2 内存数据库 (JPA) |
| LLM | DeepSeek API（无 key 时降级为关键词匹配） |
| UI | Bootstrap 5 + 自定义 CSS |
| 构建 | Maven |

## 快速开始

```bash
# 克隆项目
git clone <your-repo-url>
cd embabel-book

# 启动（不配置 DeepSeek Key 也能用，自动降级）
mvn spring-boot:run

# 打开浏览器
open http://localhost:8080
```

可选：设置 DeepSeek API Key（提升自然语言解析效果）：

```bash
export DEEPSEEK_API_KEY=sk-your-key-here
mvn spring-boot:run
```

## 演示场景

| 输入 | 预期结果 | 展示点 |
|------|---------|--------|
| "我想借科幻小说" | 成功借阅《沙丘》（三体已过滤） | OODA 动态过滤 |
| "有没有东野圭吾的书" | 成功借阅《白夜行》 | DeepSeek 作者解析 |
| "推荐机器学习的书" | 成功借阅《深度学习入门》 | 关键词搜索 |
| "我想借经济学" | 提示"没有找到" | Condition 回退 |
| 连续借两次科幻 | 第二次借到沙丘，第三次提示无书 | 动态历史累积 |

## 预置演示数据

**10 本图书**：三体、沙丘、银河帝国、白夜行、嫌疑人X的献身、百年孤独、机器学习实战、深度学习入门、统计学习方法、人类简史

**2 个用户**：
- 张三（u1）— 已借三体、百年孤独
- 李四（u2）— 未借任何书

## 项目结构

```
src/
├── main/java/com/example/library/
│   ├── LibraryApplication.java          # Spring Boot 入口
│   ├── agent/LibraryAgent.java          # OODA 循环编排
│   ├── controller/LibraryController.java # Thymeleaf Controller
│   ├── domain/                          # 领域模型 (Java record)
│   ├── action/                          # OODA Action（6 个）
│   ├── condition/                       # Condition 守卫（3 个）
│   ├── entity/                          # JPA Entity
│   ├── repository/                      # Spring Data JPA
│   ├── service/DeepSeekService.java     # DeepSeek API 封装
│   └── config/DemoDataLoader.java       # 演示数据
```

## 测试

```
mvn test
# 81 个测试，覆盖：
# - Entity 映射 14 个
# - Repository 查询 13 个
# - Condition 逻辑 12 个
# - DeepSeek 双路径 10 个
# - Action 业务 25 个
# - Agent 全链路 7 个
```

## 设计文档

- [ARCHITECTURE.md](ARCHITECTURE.md) — 架构设计
- [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) — 开发计划
- [CLAUDE.md](CLAUDE.md) — Claude Code 项目配置
