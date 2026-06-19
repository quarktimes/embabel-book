package com.example.library.domain;

public record Book(
        String id,
        String title,
        String category,
        String author,
        boolean available
) {
}
