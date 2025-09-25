package com.example.bookshop2.controller;

import com.example.bookshop2.dto.PublisherDto;
import com.example.bookshop2.service.PublisherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/publishers")
public class PublisherController {
    private final PublisherService publisherService;

    public PublisherController(PublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @Operation(summary = "Get all publishers", description = "Retrieve all publishers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all publishers")
    })
    @GetMapping
    public ResponseEntity<List<PublisherDto>> getAllPublishers() {
        return ResponseEntity.ok(publisherService.findAll());
    }

    @Operation(summary = "Get publisher by ID", description = "Retrieve a publisher by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved publisher"),
            @ApiResponse(responseCode = "404", description = "Publisher not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PublisherDto> getPublisherById(@PathVariable Long id) {
        return ResponseEntity.ok(publisherService.findById(id));
    }

    @Operation(summary = "Search publishers by name",
            description = "Search publishers by name (partial match)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved publishers"),
            @ApiResponse(responseCode = "400", description = "Invalid search name")
    })
    @GetMapping("/search")
    public ResponseEntity<List<PublisherDto>> searchPublishers(@RequestParam String name) {
        return ResponseEntity.ok(publisherService.searchByName(name));
    }

    @Operation(summary = "Create a publisher", description = "Create a new publisher")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created publisher"),
            @ApiResponse(responseCode = "400", description = "Invalid publisher data")
    })
    @PostMapping
    public ResponseEntity<PublisherDto> createPublisher(@Valid @RequestBody PublisherDto dto) {
        return ResponseEntity.ok(publisherService.create(dto));
    }

    @Operation(summary = "Update a publisher", description = "Update an existing publisher by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated publisher"),
            @ApiResponse(responseCode = "400", description = "Invalid publisher data"),
            @ApiResponse(responseCode = "404", description = "Publisher not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PublisherDto> updatePublisher(@PathVariable Long id,
                                                        @Valid @RequestBody PublisherDto dto) {
        return ResponseEntity.ok(publisherService.update(id, dto));
    }

    @Operation(summary = "Delete a publisher", description = "Delete a publisher by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted publisher"),
            @ApiResponse(responseCode = "404", description = "Publisher not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePublisher(@PathVariable Long id) {
        publisherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}