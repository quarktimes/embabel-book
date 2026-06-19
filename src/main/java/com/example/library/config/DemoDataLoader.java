package com.example.library.config;

import com.example.library.entity.BookEntity;
import com.example.library.entity.BorrowRecordEntity;
import com.example.library.entity.UserEntity;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Override
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            log.info("Demo data already loaded, skipping");
            return;
        }

        log.info("Loading demo data...");

        // ─── 10 本图书 ───
        var books = List.of(
                new BookEntity("b1", "三体", "科幻", "刘慈欣", true),
                new BookEntity("b2", "沙丘", "科幻", "弗兰克·赫伯特", true),
                new BookEntity("b3", "银河帝国：基地", "科幻", "艾萨克·阿西莫夫", true),
                new BookEntity("b4", "白夜行", "推理", "东野圭吾", true),
                new BookEntity("b5", "嫌疑人X的献身", "推理", "东野圭吾", true),
                new BookEntity("b6", "百年孤独", "文学", "加西亚·马尔克斯", true),
                new BookEntity("b7", "机器学习实战", "计算机", "Peter Harrington", true),
                new BookEntity("b8", "深度学习入门", "计算机", "斋藤康毅", true),
                new BookEntity("b9", "统计学习方法", "计算机", "李航", false),        // 不可借
                new BookEntity("b10", "人类简史", "历史", "尤瓦尔·赫拉利", true)
        );
        bookRepository.saveAll(books);

        // ─── 2 个用户 ───
        var user1 = new UserEntity("u1", "张三");
        var user2 = new UserEntity("u2", "李四");
        userRepository.save(user1);
        userRepository.save(user2);

        // ─── 张三已借三体 + 百年孤独 ───
        var sanTi = bookRepository.findById("b1").orElseThrow();
        sanTi.setAvailable(false);
        bookRepository.save(sanTi);

        var baiNian = bookRepository.findById("b6").orElseThrow();
        baiNian.setAvailable(false);
        bookRepository.save(baiNian);

        borrowRecordRepository.save(new BorrowRecordEntity(user1, sanTi, LocalDateTime.of(2026, 6, 1, 10, 0)));
        borrowRecordRepository.save(new BorrowRecordEntity(user1, baiNian, LocalDateTime.of(2026, 6, 5, 14, 30)));

        log.info("Demo data loaded: {} books, {} users, {} borrow records",
                bookRepository.count(), userRepository.count(), borrowRecordRepository.count());
    }
}
