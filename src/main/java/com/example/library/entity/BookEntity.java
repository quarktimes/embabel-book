package com.example.library.entity;

import com.example.library.domain.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class BookEntity {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String author;

    @Column(nullable = false)
    private boolean available = true;

    public BookEntity() {
    }

    public BookEntity(String id, String title, String category, String author, boolean available) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.author = author;
        this.available = available;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public Book toDomain() {
        return new Book(id, title, category, author, available);
    }

    public static BookEntity fromDomain(Book book) {
        return new BookEntity(
                book.id(),
                book.title(),
                book.category(),
                book.author(),
                book.available()
        );
    }
}
