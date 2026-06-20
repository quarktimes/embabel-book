package com.example.library.service;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.example.library.domain.Book;
import com.example.library.domain.BorrowRequest;
import com.example.library.domain.BorrowResult;
import com.example.library.entity.BookEntity;
import com.example.library.entity.BorrowRecordEntity;
import com.example.library.entity.UserEntity;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 图书借阅视图服务。
 * 通过 AgentPlatform 执行 Embabel GOAP 规划器自动编排 Action 链。
 * 失败时根据异常码返回对应中文提示。
 */
@Service
public class LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);

    @Autowired
    private AgentPlatform agentPlatform;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    public List<Book> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(BookEntity::toDomain)
                .toList();
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public List<BorrowRecordEntity> getBorrowHistory(String userId) {
        return borrowRecordRepository.findByUserIdOrderByBorrowTimeDesc(userId);
    }

    public String borrowBook(String userId, String query) {
        // 输入校验 — 无需 GOAP
        if (query == null || query.isBlank()) {
            return "请描述您想借什么样的书，比如「我想借科幻小说」";
        }

        try {
            var agent = agentPlatform.agents().stream()
                    .filter(a -> "LibraryAgent".equals(a.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("LibraryAgent not found"));

            var process = agentPlatform.createAgentProcessFrom(
                    agent, ProcessOptions.DEFAULT, new BorrowRequest(userId, query));
            CompletableFuture<?> future = agentPlatform.start(process);
            future.get();

            BorrowResult result = process.resultOfType(BorrowResult.class);
            if (result != null) {
                return "成功借阅《" + result.book().title() + "》(" + result.book().author() + ")";
            }

            // 执行完毕但没有结果 → GOAP 规划器未能完成（Condition 不满足）
            return "请描述您想借什么样的书，比如「我想借科幻小说」";

        } catch (java.util.concurrent.ExecutionException e) {
            return handleError(e.getCause());
        } catch (Exception e) {
            return handleError(e);
        }
    }

    private String handleError(Throwable t) {
        if (t instanceof IllegalArgumentException ia) {
            return switch (ia.getMessage()) {
                case "user_not_found" -> "用户不存在";
                default -> "抱歉，请求有误";
            };
        }
        if (t instanceof IllegalStateException is) {
            return switch (is.getMessage()) {
                case "no_books_found" -> "抱歉，没有找到符合的图书";
                case "all_borrowed" -> "找到的图书目前都已被借出";
                case "all_borrowed_before" -> "这些书您都已经借过了";
                default -> "抱歉，暂时无法借阅";
            };
        }
        log.error("unexpected error: {}", t.getMessage(), t);
        return "抱歉，借书服务异常";
    }
}
