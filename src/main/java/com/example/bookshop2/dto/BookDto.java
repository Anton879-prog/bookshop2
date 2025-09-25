package com.example.bookshop2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import lombok.Data;

@Data
public class BookDto {
    private Long id;
    @NotBlank(message = "Name is required for creation")
    private String name;
    private String genre;
    @Positive(message = "Price must be positive")
    private Double price;
    private Long publisherId;
    private String publisherName;
    private Set<Long> authorIds;
    private Set<String> authorNames;
}