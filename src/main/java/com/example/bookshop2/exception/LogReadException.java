package com.example.bookshop2.exception;

public class LogReadException extends RuntimeException {
    public LogReadException(String message, Throwable cause) {
        super(message, cause);
    }
}