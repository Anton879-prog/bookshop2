package com.example.bookshop2.repository;

import com.example.bookshop2.model.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByPublisherId(Long publisherId);

    @Query(value = "SELECT b.id, b.name, b.genre, b.price, b.publisher_id FROM books b "
            + "JOIN publishers p ON b.publisher_id = p.id "
            + "WHERE p.name = :publisherName", nativeQuery = true)
    List<Book> findByPublisherNameNative(@Param("publisherName") String publisherName);

    List<Book> findByPriceBetween(Double minPrice, Double maxPrice);
}