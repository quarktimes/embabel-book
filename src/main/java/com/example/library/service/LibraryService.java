package com.example.library.service;

import com.example.library.agent.LibraryAgent;
import com.example.library.domain.Book;
import com.example.library.domain.BorrowRequest;
import com.example.library.entity.BookEntity;
import com.example.library.entity.BorrowRecordEntity;
import com.example.library.entity.UserEntity;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图书借阅的视图层服务。
 * 负责组装 Controller 需要的所有视图数据，隐藏 Repository 细节。
 */
@Service
public class LibraryService {

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
        return agent.execute(new BorrowRequest(userId, query));
    }
}
