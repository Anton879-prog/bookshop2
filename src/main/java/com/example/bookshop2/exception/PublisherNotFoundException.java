package com.example.bookshop2.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PublisherNotFoundException extends RuntimeException {
    public PublisherNotFoundException(Long id) {
        super("Publisher with ID " + id + " not found");
    }

    public PublisherNotFoundException(String name) {
        super("Publisher with name " + name + " not found");
    }
}