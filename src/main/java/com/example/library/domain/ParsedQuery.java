package com.example.library.domain;

public record ParsedQuery(
        String category,
        String keyword,
        String author
) {
}
