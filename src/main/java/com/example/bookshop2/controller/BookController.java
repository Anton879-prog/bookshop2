package com.example.bookshop2.controller;

import com.example.bookshop2.dto.BookDto;
import com.example.bookshop2.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
@Tag(name = "Books", description = "API для работы с книгами")
public class BookController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookController.class);
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    private String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[\\r\\n\\t]", "_")
                .replaceAll("[^\\x20-\\x7E]", "");
    }

    @Operation(summary = "Get all books", description = "Retrieve all books in the bookstore")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all books")
    })
    @GetMapping
    public ResponseEntity<List<BookDto>> getAllBooks() {
        LOGGER.info("Fetching all books");
        return ResponseEntity.ok(bookService.findAll());
    }

    @Operation(summary = "Get book by ID", description = "Retrieve a book by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved book"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(@PathVariable Long id) {
        LOGGER.info("Fetching book with ID: {}", id);
        return ResponseEntity.ok(bookService.findById(id));
    }

    @Operation(summary = "Get books by publisher ID", description = "Retrieve all books by a publisher's ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved books"),
            @ApiResponse(responseCode = "404", description = "Publisher not found")
    })
    @GetMapping("/publisher")
    public ResponseEntity<List<BookDto>> getBooksByPublisherId(@RequestParam Long publisherId) {
        LOGGER.info("Querying books for publisher ID: {}", publisherId);
        return ResponseEntity.ok(bookService.findByPublisherId(publisherId));
    }

    @Operation(summary = "Get books by publisher name",
            description = "Retrieve all books by a publisher's name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved books"),
            @ApiResponse(responseCode = "404", description = "Publisher not found")
    })
    @GetMapping("/publisher_name")
    public ResponseEntity<List<BookDto>> getBooksByPublisherName(@RequestParam String publisherName) {
        String sanitizedName = sanitize(publisherName);
        LOGGER.info("Querying books for publisher name: {}", sanitizedName);
        return ResponseEntity.ok(bookService.findByPublisherName(publisherName));
    }

    @Operation(summary = "Create a book", description = "Create a new book")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created book"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping
    public ResponseEntity<BookDto> createBook(@Valid @RequestBody BookDto dto) {
        String sanitizedTitle = sanitize(dto.getName());
        LOGGER.info("Creating book with title: {}", sanitizedTitle);
        return ResponseEntity.ok(bookService.create(dto));
    }

    @Operation(summary = "Update a book", description = "Update an existing book by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated book"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BookDto> updateBook(@PathVariable Long id, @Valid @RequestBody BookDto dto) {
        String sanitizedTitle = sanitize(dto.getName());
        LOGGER.info("Updating book with ID: {}, title: {}", id, sanitizedTitle);
        return ResponseEntity.ok(bookService.update(id, dto));
    }

    @Operation(summary = "Delete a book", description = "Delete a book by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted book"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        LOGGER.info("Deleting book with ID: {}", id);
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get books by price range",
            description = "Retrieve books within a specified price range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved books"),
            @ApiResponse(responseCode = "400", description = "Invalid price range")
    })
    @GetMapping("/price-range")
    public ResponseEntity<List<BookDto>> getBooksByPriceRange(
            @Parameter(description = "Minimum price") @RequestParam Double minPrice,
            @Parameter(description = "Maximum price") @RequestParam Double maxPrice) {
        LOGGER.info("Fetching books with price range: {} - {}", minPrice, maxPrice);
        return ResponseEntity.ok(bookService.findByPriceRange(minPrice, maxPrice));
    }

    @Operation(summary = "Add multiple books", description = "Add multiple books in a single request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added books"),
            @ApiResponse(responseCode = "400", description = "Invalid book data")
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<BookDto>> addBooksBulk(
            @Parameter(description = "List of books to add") @Valid @RequestBody List<BookDto> books) {
        LOGGER.info("Adding {} books", books.size());
        return ResponseEntity.ok(bookService.addBooksBulk(books));
    }
}