package com.example.bookshop2.mapper;

import com.example.bookshop2.dto.BookDto;
import com.example.bookshop2.model.Author;
import com.example.bookshop2.model.Book;
import com.example.bookshop2.model.Publisher;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class BookMapper {
    private BookMapper() {}

    public static BookDto toDto(Book book) {
        BookDto dto = new BookDto();
        dto.setId(book.getId());
        dto.setName(book.getName());
        dto.setGenre(book.getGenre());
        dto.setPrice(book.getPrice());
        Publisher publisher = book.getPublisher();
        if (publisher != null) {
            dto.setPublisherId(publisher.getId());
            dto.setPublisherName(publisher.getName());
        }
        Set<Author> authors = book.getAuthors();
        if (authors != null) {
            dto.setAuthorIds(authors.stream().map(Author::getId).collect(Collectors.toSet()));
            dto.setAuthorNames(authors.stream().map(Author::getName).collect(Collectors.toSet()));
        } else {
            dto.setAuthorIds(Collections.emptySet());
            dto.setAuthorNames(Collections.emptySet());
        }
        return dto;
    }

    public static Book fromDto(BookDto dto, Set<Author> authors, Publisher publisher) {
        Book book = new Book();
        book.setName(dto.getName());
        book.setGenre(dto.getGenre());
        book.setPrice(dto.getPrice());
        book.setAuthors(authors);
        book.setPublisher(publisher);
        return book;
    }

    public static void updateFromDto(Book book, BookDto dto, Set<Author> authors, Publisher publisher) {
        if (dto.getName() != null) {
            book.setName(dto.getName());
        }
        if (dto.getGenre() != null) {
            book.setGenre(dto.getGenre());
        }
        if (dto.getPrice() != null) {
            book.setPrice(dto.getPrice());
        }
        if (publisher != null) {
            book.setPublisher(publisher);
        }
        if (authors != null) {
            book.setAuthors(authors);
        }
    }
}