package com.example.bookshop2.repository;

import com.example.bookshop2.model.Author;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByName(String name);

    List<Author> findByNameContainingIgnoreCase(String name);
}