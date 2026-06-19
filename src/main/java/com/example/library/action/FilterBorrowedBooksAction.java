package com.example.library.action;

import com.example.library.domain.Book;
import com.example.library.domain.User;
import com.example.library.condition.NotBorrowedBeforeCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FilterBorrowedBooksAction {

    private static final Logger log = LoggerFactory.getLogger(FilterBorrowedBooksAction.class);

    @Autowired
    private NotBorrowedBeforeCondition notBorrowedBeforeCondition;

    /**
     * 过滤掉用户已借过的书。
     */
    public List<Book> execute(List<Book> books, User user) {
        if (books == null || books.isEmpty()) {
            return List.of();
        }
        var notBorrowed = books.stream()
                .filter(b -> notBorrowedBeforeCondition.test(user, b))
                .toList();
        log.info("action=FilterBorrowedBooks cost=0 input={} afterFilter={}", books.size(), notBorrowed.size());
        return notBorrowed;
    }
}
