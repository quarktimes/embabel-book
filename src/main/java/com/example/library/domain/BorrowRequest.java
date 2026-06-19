package com.example.library.domain;

public record BorrowRequest(
        String userId,
        String query
) {
}
