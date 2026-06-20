package com.example.library.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;
import com.example.library.domain.Book;
import com.example.library.domain.BorrowRequest;
import com.example.library.domain.BorrowResult;
import com.example.library.domain.ParsedQuery;
import com.example.library.domain.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;
import com.example.library.entity.BorrowRecordEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Embabel 智能图书借阅 Agent。
 * <p>
 * 所有 OODA 步骤通过 @Action 注解标记，Conditions 由
 * Embabel GOAP 规划器自动评估，不再需要 if/else 手动编排。
 * <p>
 * LibraryService 通过 AgentPlatform API 执行 Agent，
 * 框架自动按数据流 + Conditions 规划执行顺序。
 */
@Component
@Agent(
        name = "LibraryAgent",
        description = "智能图书借阅 Agent — 通过自然语言完成图书借阅",
        version = "1.0.0"
)
public class LibraryAgent {

    private static final Logger log = LoggerFactory.getLogger(LibraryAgent.class);

    @Autowired
    private Ai ai;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    // ════════════════════════════════════════════
    // Actions — 每个步骤用 @Action 标记，Conditions 由 Embabel 评估
    // ════════════════════════════════════════════

    @Action(cost = 5, description = "检查并解析用户查询",
            pre = "hasValidQuery")
    public ParsedQuery parseQuery(BorrowRequest request) {
        log.info("action=parseQuery request={}", request.query());
        var llm = LlmOptions.withModel("deepseek-chat").withTemperature(0.1);
        return ai.withLlm(llm)
                .withSystemPrompt("""
                        将用户的自然语言查询解析为结构化信息。
                        返回 JSON：{category, keyword, author}，null 表示无。
                        示例：「我想借科幻小说」→ {"category":"科幻","keyword":null,"author":null}
                        """)
                .creating(ParsedQuery.class)
                .fromPrompt(request.query());
    }

    @Action(cost = 2, description = "按分类/作者/关键词搜索图书")
    public List<Book> searchBooks(ParsedQuery parsed) {
        log.info("action=searchBooks parsed={}", parsed);
        if (parsed.category() != null)
            return bookRepository.findByCategoryContaining(parsed.category()).stream().map(e -> e.toDomain()).toList();
        if (parsed.author() != null)
            return bookRepository.findByAuthorContaining(parsed.author()).stream().map(e -> e.toDomain()).toList();
        if (parsed.keyword() != null)
            return bookRepository.findByTitleContaining(parsed.keyword()).stream().map(e -> e.toDomain()).toList();
        return List.of();
    }

    @Transactional
    @Action(cost = 1, description = "检查可借、过滤已借、执行借书")
    @AchievesGoal(description = "用户成功借到图书", value = 1.0)
    public BorrowResult borrowBook(List<Book> books, BorrowRequest request) {
        log.info("action=borrowBook input={} books", books.size());
        if (books == null || books.isEmpty())
            throw new IllegalStateException("no_books_found");

        var user = userRepository.findById(request.userId())
                .map(e -> e.toDomain())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        // 实时检查可借 + 过滤已借
        var candidates = books.stream()
                .filter(b -> bookRepository.findById(b.id())
                        .map(e -> e.isAvailable()).orElse(false))
                .filter(b -> !user.borrowedBookIds().contains(b.id()))
                .toList();

        if (candidates.isEmpty())
            throw new IllegalStateException("all_borrowed");

        var book = candidates.getFirst();
        bookRepository.findById(book.id()).ifPresent(e -> e.setAvailable(false));
        bookRepository.flush();
        borrowRecordRepository.save(new BorrowRecordEntity(
                userRepository.findById(user.id()).orElseThrow(),
                bookRepository.findById(book.id()).orElseThrow(),
                LocalDateTime.now()));
        return new BorrowResult(true, "成功借阅《" + book.title() + "》(" + book.author() + ")", book);
    }

    // ════════════════════════════════════════════
    // Conditions — 给 Embabel 规划器用
    // ════════════════════════════════════════════

    @Condition(name = "hasValidQuery", cost = 0)
    public boolean hasValidQuery(BorrowRequest request) {
        return request != null && request.query() != null && !request.query().isBlank();
    }

}
