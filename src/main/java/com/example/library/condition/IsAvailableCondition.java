package com.example.library.condition;

import com.example.library.domain.Book;
import com.example.library.entity.BookEntity;
import com.example.library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 图书是否可借 — 每次重新读库，确保实时性。
 * 这是 OODA 动态性的关键体现：两次调用可能返回不同结果。
 */
@Component
public class IsAvailableCondition {

    @Autowired
    private BookRepository bookRepository;

    public boolean test(Book book) {
        return bookRepository.findById(book.id())
                .map(BookEntity::isAvailable)
                .orElse(false);
    }
}
