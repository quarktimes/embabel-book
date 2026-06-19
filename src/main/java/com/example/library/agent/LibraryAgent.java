package com.example.library.agent;

import com.example.library.action.CheckAvailabilityAction;
import com.example.library.action.ExecuteBorrowAction;
import com.example.library.action.FilterBorrowedBooksAction;
import com.example.library.action.ParseQueryAction;
import com.example.library.action.ReturnResultAction;
import com.example.library.action.SearchBooksAction;
import com.example.library.condition.HasValidQueryCondition;
import com.example.library.domain.Book;
import com.example.library.domain.BorrowRequest;
import com.example.library.domain.ParsedQuery;
import com.example.library.domain.User;
import com.example.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Embabel OODA 循环编排器。
 * <p>
 * 按 Observe → Orient → Decide → Act 顺序执行：
 * <pre>
 * [Observe] 接收 BorrowRequest
 * [Orient]  ParseQuery → 解析自然语言
 * [Decide]  SearchBooks → CheckAvailable → FilterBorrowed
 * [Act]     ExecuteBorrow → ReturnResult
 * </pre>
 */
@Component
public class LibraryAgent {

    private static final Logger log = LoggerFactory.getLogger(LibraryAgent.class);

    @Autowired
    private HasValidQueryCondition hasValidQueryCondition;

    @Autowired
    private ParseQueryAction parseQueryAction;

    @Autowired
    private SearchBooksAction searchBooksAction;

    @Autowired
    private CheckAvailabilityAction checkAvailabilityAction;

    @Autowired
    private FilterBorrowedBooksAction filterBorrowedBooksAction;

    @Autowired
    private ExecuteBorrowAction executeBorrowAction;

    @Autowired
    private ReturnResultAction returnResultAction;

    @Autowired
    private UserRepository userRepository;

    /**
     * 执行一次完整的 OODA 循环。
     *
     * @param request 借书请求（用户 ID + 自然语言查询）
     * @return 给用户的回复信息
     */
    public String execute(BorrowRequest request) {
        log.info("=== OODA Loop Start ===");

        // ═══════════════════════════════════════
        // [Observe] 接收请求
        // ═══════════════════════════════════════
        log.info("[Observe] request user={} query={}", request.userId(), request.query());

        // ═══════════════════════════════════════
        // [Orient] 理解意图
        // ═══════════════════════════════════════
        if (!hasValidQueryCondition.test(request)) {
            log.info("[Orient] HasValidQueryCondition=false → INFORM_USER");
            return "请描述您想借什么样的书，比如「我想借科幻小说」或「有没有东野圭吾的书」";
        }

        ParsedQuery parsed = parseQueryAction.execute(request);
        log.info("[Orient] parsedQuery={}", parsed);

        // ═══════════════════════════════════════
        // [Decide] 搜索 → 检查可用 → 过滤已借
        // ═══════════════════════════════════════
        List<Book> allBooks = searchBooksAction.execute(parsed);
        if (allBooks.isEmpty()) {
            log.info("[Decide] searchBooks=0 → INFORM_USER");
            return "抱歉，没有找到" + describeQuery(parsed) + "的图书";
        }
        log.info("[Decide] searchBooks={}", allBooks.size());

        List<Book> available = checkAvailabilityAction.execute(allBooks);
        if (available.isEmpty()) {
            log.info("[Decide] available=0 → INFORM_USER");
            return "找到的图书目前都已被借出，请稍后再来或换个分类试试";
        }
        log.info("[Decide] available={}", available.size());

        User user = userRepository.findById(request.userId())
                .map(e -> e.toDomain())
                .orElse(null);
        if (user == null) {
            return "用户不存在";
        }

        List<Book> candidates = filterBorrowedBooksAction.execute(available, user);
        if (candidates.isEmpty()) {
            log.info("[Decide] candidates=0 (all borrowed before) → INFORM_USER");
            return "这些书您都已经借过了，推荐看看其他分类吧";
        }
        log.info("[Decide] candidates={}", candidates.size());

        // 选第一本借阅
        Book selected = candidates.get(0);
        log.info("[Decide] selected={}", selected.title());

        // ═══════════════════════════════════════
        // [Act] 执行借书 → 返回结果
        // ═══════════════════════════════════════
        var result = executeBorrowAction.execute(selected, user);
        var message = returnResultAction.execute(result);

        log.info("[Act] result={}", message);
        log.info("=== OODA Loop End ===");
        return message;
    }

    private String describeQuery(ParsedQuery q) {
        if (q.category() != null) return q.category() + "类";
        if (q.author() != null) return q.author() + "的作品";
        if (q.keyword() != null) return "关于「" + q.keyword() + "」";
        return "";
    }
}
