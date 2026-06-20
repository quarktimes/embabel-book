package com.example.library.service;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.example.library.agent.LibraryAgent;
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
 * 图书借阅的视图层服务。
 * <p>
 * 主线：通过 AgentPlatform 执行 GOAP agent，引擎自主编排 Action 链。
 * 降级：Agent 异常时直调 execute()。
 */
@Service
public class LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);

    @Autowired
    private AgentPlatform agentPlatform;

    @Autowired
    private LibraryAgent agent;

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
        // ── 主线：通过 GOAP 引擎执行 Agent ──
        try {
            Agent platformAgent = agentPlatform.agents().stream()
                    .filter(a -> "LibraryAgent".equals(a.getName()))
                    .findFirst()
                    .orElse(null);

            if (platformAgent != null) {
                var request = new BorrowRequest(userId, query);
                var process = agentPlatform.createAgentProcessFrom(
                        platformAgent, ProcessOptions.DEFAULT, request);
                CompletableFuture<?> future = agentPlatform.start(process);
                future.get();

                // 从黑板提取借书结果
                BorrowResult result = process.resultOfType(BorrowResult.class);
                if (result != null) {
                    return "成功借阅《" + result.book().title() + "》(" + result.book().author() + ")";
                }
            }
        } catch (Exception e) {
            log.warn("AgentPlatform execution failed, falling back to execute(): {}", e.getMessage());
        }

        // ── 降级：手动编排 ──
        try {
            var result = agent.execute(new BorrowRequest(userId, query));
            return "成功借阅《" + result.book().title() + "》(" + result.book().author() + ")";
        } catch (IllegalArgumentException e) {
            return switch (e.getMessage()) {
                case "empty_query" -> "请描述您想借什么样的书，比如「我想借科幻小说」";
                case "user_not_found" -> "用户不存在";
                default -> "抱歉，请求有误";
            };
        } catch (IllegalStateException e) {
            return switch (e.getMessage()) {
                case "no_books_found" -> "抱歉，没有找到符合的图书";
                case "all_borrowed" -> "找到的图书目前都已被借出";
                case "all_borrowed_before" -> "这些书您都已经借过了";
                default -> "抱歉，暂时无法借阅";
            };
        }
    }
}
