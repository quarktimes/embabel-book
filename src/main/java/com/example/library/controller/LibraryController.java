package com.example.library.controller;

import com.example.library.agent.LibraryAgent;
import com.example.library.domain.BorrowRequest;
import com.example.library.entity.BookEntity;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LibraryController {

    @Autowired
    private LibraryAgent agent;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "u1") String userId, Model model) {
        model.addAttribute("books", bookRepository.findAll().stream()
                .map(BookEntity::toDomain).toList());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("borrowRecords",
                borrowRecordRepository.findByUserIdOrderByBorrowTimeDesc(userId));
        return "index";
    }

    @GetMapping("/books")
    public String books(@RequestParam(defaultValue = "u1") String userId, Model model) {
        model.addAttribute("books", bookRepository.findAll().stream()
                .map(BookEntity::toDomain).toList());
        model.addAttribute("borrowRecords",
                borrowRecordRepository.findByUserIdOrderByBorrowTimeDesc(userId));
        return "fragments/bookList :: bookList";
    }

    @PostMapping("/borrow")
    public String borrow(@RequestParam String userId,
                         @RequestParam String query,
                         Model model) {
        var request = new BorrowRequest(userId, query);
        String message = agent.execute(request);

        model.addAttribute("message", message);
        model.addAttribute("books", bookRepository.findAll().stream()
                .map(BookEntity::toDomain).toList());
        model.addAttribute("borrowRecords",
                borrowRecordRepository.findByUserIdOrderByBorrowTimeDesc(userId));

        return "fragments/borrowResult :: borrowResult";
    }
}
