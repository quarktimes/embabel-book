package com.example.library.agent;

import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.example.library.action.CheckAvailabilityAction;
import com.example.library.action.ExecuteBorrowAction;
import com.example.library.action.FilterBorrowedBooksAction;
import com.example.library.action.ParseQueryAction;
import com.example.library.action.ReturnResultAction;
import com.example.library.action.SearchBooksAction;
import com.example.library.domain.Book;
import com.example.library.domain.BorrowRequest;
import com.example.library.domain.BorrowResult;
import com.example.library.domain.ParsedQuery;
import com.example.library.domain.User;
import com.example.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Embabel 智能图书借阅 Agent。
 * <p>
 * 使用 Embabel 注解标记完整的 OODA 结构：
 * <ul>
 *   <li>{@code @Agent} — 声明 Agent</li>
 *   <li>{@code @Action} — OODA 步骤，{@code pre} 引用 Condition</li>
 *   <li>{@code @Condition} — 守卫条件，Embabel 框架自动评估</li>
 *   <li>{@code @AchievesGoal} — 标记目标达成</li>
 * </ul>
 * <p>
 * 当前为手动编排模式。当 AgentPlatformAutoConfiguration 启用后，
 * Embabel 的 GOAP 规划器会根据 {@code @Action(pre = "条件名")}
 * 和数据流类型自动编排执行顺序，不再需要 {@code execute()} 方法。
 */
@Component
@Agent(
        name = "LibraryAgent",
        description = "智能图书借阅 Agent — 通过自然语言完成图书借阅",
        version = "1.0"
)
public class LibraryAgent {

    private static final Logger log = LoggerFactory.getLogger(LibraryAgent.class);

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

    // ════════════════════════════════════════════
    // Conditions — 由 Embabel 规划器自动评估
    // ════════════════════════════════════════════

    @Condition(name = "hasValidQuery", cost = 0)
    public boolean hasValidQuery(BorrowRequest request) {
        return request != null && request.query() != null && !request.query().isBlank();
    }

    @Condition(name = "matchFound", cost = 0)
    public boolean matchFound(List<Book> books) {
        return books != null && !books.isEmpty();
    }

    @Condition(name = "availableExists", cost = 0)
    public boolean availableExists(List<Book> books) {
        return books != null && !books.isEmpty();
    }

    @Condition(name = "unborrowedExists", cost = 0)
    public boolean unborrowedExists(List<Book> books) {
        return books != null && !books.isEmpty();
    }

    // ════════════════════════════════════════════
    // OODA 循环 — 手动编排（平台启用后由 GOAP 接管）
    // ════════════════════════════════════════════

    /**
     * 执行一次完整的 OODA 循环。
     * <p>
     * 当前为手动编排。各 Condition 由 {@code @Condition} 注解标记，
     * Action 由 {@code @Action} 注解标记。
     * 当 AgentPlatform 启用后，此方法可由框架自动生成，
     * 通过 {@code @Action(pre = "conditionName")} + 数据流推断。
     */
    public String execute(BorrowRequest request) {
        log.info("=== OODA Loop Start ===");

        // ═══ [Observe] ═══
        log.info("[Observe] request user={} query={}", request.userId(), request.query());

        // ═══ [Orient] ═══
        if (!hasValidQuery(request)) {
            return "请描述您想借什么样的书，比如「我想借科幻小说」或「有没有东野圭吾的书」";
        }

        ParsedQuery parsed = parseQueryAction.execute(request);
        log.info("[Orient] parsed={}", parsed);

        // ═══ [Decide] ═══
        List<Book> allBooks = searchBooksAction.execute(parsed);
        if (!matchFound(allBooks)) {
            return "抱歉，没有找到" + describeQuery(parsed) + "的图书";
        }
        log.info("[Decide] searchBooks={}", allBooks.size());

        List<Book> available = checkAvailabilityAction.execute(allBooks);
        if (!availableExists(available)) {
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
        if (!unborrowedExists(candidates)) {
            return "这些书您都已经借过了，推荐看看其他分类吧";
        }
        log.info("[Decide] candidates={}", candidates.size());

        Book selected = candidates.getFirst();
        log.info("[Decide] selected={}", selected.title());

        // ═══ [Act] ═══
        BorrowResult result = executeBorrowAction.execute(selected, user);
        String message = returnResultAction.execute(result);

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
