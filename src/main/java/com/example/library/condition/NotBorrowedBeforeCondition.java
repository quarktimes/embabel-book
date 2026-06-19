package com.example.library.condition;

import com.example.library.domain.Book;
import com.example.library.domain.User;
import org.springframework.stereotype.Component;

/**
 * 用户是否没有借过这本书 — 纯内存比较。
 */
@Component
public class NotBorrowedBeforeCondition {

    public boolean test(User user, Book book) {
        if (user == null || user.borrowedBookIds() == null) {
            return true;
        }
        return !user.borrowedBookIds().contains(book.id());
    }
}
