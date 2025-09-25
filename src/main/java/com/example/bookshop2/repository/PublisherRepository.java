package com.example.bookshop2.repository;

import com.example.bookshop2.model.Publisher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    Optional<Publisher> findByName(String name);

    List<Publisher> findByNameContainingIgnoreCase(String name);
}