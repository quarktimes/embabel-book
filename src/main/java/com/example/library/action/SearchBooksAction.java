package com.example.library.action;

import com.example.library.domain.Book;
import com.example.library.domain.ParsedQuery;
import com.example.library.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchBooksAction {

    private static final Logger log = LoggerFactory.getLogger(SearchBooksAction.class);

    @Autowired
    private BookRepository bookRepository;

    /**
     * 按优先级搜索：category → author → keyword。
     * 三者都 null 时返回空列表。
     */
    public List<Book> execute(ParsedQuery parsed) {
        if (parsed.category() != null && !parsed.category().isBlank()) {
            log.info("action=SearchBooks cost=2 by=category value={}", parsed.category());
            return bookRepository.findByCategoryContaining(parsed.category())
                    .stream().map(e -> e.toDomain()).toList();
        }
        if (parsed.author() != null && !parsed.author().isBlank()) {
            log.info("action=SearchBooks cost=2 by=author value={}", parsed.author());
            return bookRepository.findByAuthorContaining(parsed.author())
                    .stream().map(e -> e.toDomain()).toList();
        }
        if (parsed.keyword() != null && !parsed.keyword().isBlank()) {
            log.info("action=SearchBooks cost=2 by=keyword value={}", parsed.keyword());
            return bookRepository.findByTitleContaining(parsed.keyword())
                    .stream().map(e -> e.toDomain()).toList();
        }
        return List.of();
    }
}
