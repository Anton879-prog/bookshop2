package com.example.bookshop2.controller;

import com.example.bookshop2.service.VisitCounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visit")
@RequiredArgsConstructor
@Tag(name = "Visit Counter", description = "API для статистики посещений")
public class VisitController {
    private final VisitCounterService visitCounterService;

    @Operation(summary = "Получить статистику посещений",
            description = "Возвращает количество посещений для каждого URL")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Integer>> getAllStats() {
        return ResponseEntity.ok(visitCounterService.getAllCounts());
    }
}