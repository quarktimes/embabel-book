package com.example.library.domain;

public record BorrowResult(
        boolean success,
        String message,
        Book book
) {
}
