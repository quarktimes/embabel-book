package com.example.library.controller;

import com.example.library.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Thymeleaf Controller，只处理 HTTP 和视图组装。
 * 所有业务逻辑委托给 LibraryService。
 */
@Controller
public class LibraryController {

    @Autowired
    private LibraryService libraryService;

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "u1") String userId, Model model) {
        model.addAttribute("books", libraryService.getAllBooks());
        model.addAttribute("users", libraryService.getAllUsers());
        model.addAttribute("borrowRecords", libraryService.getBorrowHistory(userId));
        return "index";
    }

    @GetMapping("/books")
    public String books(@RequestParam(defaultValue = "u1") String userId, Model model) {
        model.addAttribute("books", libraryService.getAllBooks());
        model.addAttribute("borrowRecords", libraryService.getBorrowHistory(userId));
        return "fragments/bookList :: bookList";
    }

    @PostMapping("/borrow")
    public String borrow(@RequestParam String userId,
                         @RequestParam String query,
                         Model model) {
        String message = libraryService.borrowBook(userId, query);

        model.addAttribute("message", message);
        model.addAttribute("books", libraryService.getAllBooks());
        model.addAttribute("borrowRecords", libraryService.getBorrowHistory(userId));

        return "fragments/borrowResult :: borrowResult";
    }
}
