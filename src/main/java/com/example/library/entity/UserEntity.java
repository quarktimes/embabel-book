package com.example.library.entity;

import com.example.library.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "user")
    private List<BorrowRecordEntity> borrowRecords = new ArrayList<>();

    public UserEntity() {
    }

    public UserEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<BorrowRecordEntity> getBorrowRecords() { return borrowRecords; }
    public void setBorrowRecords(List<BorrowRecordEntity> borrowRecords) {
        this.borrowRecords = borrowRecords;
    }

    /**
     * 转领域对象，只包含未归还的图书 ID。
     * returnTime == null 表示未归还。
     */
    public User toDomain() {
        var bookIds = borrowRecords.stream()
                .filter(r -> r.getReturnTime() == null)
                .map(r -> r.getBook().getId())
                .toList();
        return new User(id, name, bookIds);
    }
}
