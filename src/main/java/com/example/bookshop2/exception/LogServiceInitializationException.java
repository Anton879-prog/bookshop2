package com.example.bookshop2.exception;

public class LogServiceInitializationException extends RuntimeException {
    public LogServiceInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}