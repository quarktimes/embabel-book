package com.example.library.action;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.example.library.domain.Book;
import com.example.library.domain.BorrowResult;
import com.example.library.domain.User;
import com.example.library.entity.BookEntity;
import com.example.library.entity.BorrowRecordEntity;
import com.example.library.entity.UserEntity;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ExecuteBorrowAction {

    private static final Logger log = LoggerFactory.getLogger(ExecuteBorrowAction.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    /**
     * 执行借书 — 事务性操作。
     * 执行前二次确认图书可借，确保并发安全。
     */
    @Transactional
    @Action(cost = 1, description = "执行借书操作，更新图书状态并创建借书记录")
    @AchievesGoal(description = "用户成功借到图书", value = 1.0)
    public BorrowResult execute(Book book, User user) {
        // 二次确认：重新读库检查 available 状态
        var bookEntity = bookRepository.findById(book.id())
                .orElseThrow(() -> new IllegalArgumentException("图书不存在: " + book.id()));

        if (!bookEntity.isAvailable()) {
            throw new IllegalStateException("图书已被借出: " + book.title());
        }

        // 更新图书状态
        bookEntity.setAvailable(false);
        bookRepository.save(bookEntity);

        // 创建借书记录
        var userEntity = userRepository.findById(user.id())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + user.id()));

        var record = new BorrowRecordEntity(userEntity, bookEntity, LocalDateTime.now());
        borrowRecordRepository.save(record);

        log.info("action=ExecuteBorrow cost=1 book={} user={}", book.id(), user.id());

        var message = "成功为您借阅《" + book.title() + "》(" + book.author() + ")";
        return new BorrowResult(true, message, book);
    }
}
