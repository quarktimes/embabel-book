package com.example.library.repository;

import com.example.library.entity.BorrowRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecordEntity, String> {

    List<BorrowRecordEntity> findByUserIdOrderByBorrowTimeDesc(String userId);
}
