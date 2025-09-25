package com.example.bookshop2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthorDto {
    private Long id;
    @NotBlank(message = "Name is required for creation")
    private String name;
}