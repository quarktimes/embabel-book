package com.example.library.repository;

import com.example.library.entity.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<BookEntity, String> {

    List<BookEntity> findByCategoryContaining(String category);

    List<BookEntity> findByAuthorContaining(String author);

    List<BookEntity> findByTitleContaining(String keyword);
}
