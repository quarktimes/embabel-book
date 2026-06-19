package com.example.library.domain;

import java.util.List;

public record User(
        String id,
        String name,
        List<String> borrowedBookIds
) {
}
