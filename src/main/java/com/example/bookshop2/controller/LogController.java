package com.example.bookshop2.controller;

import com.example.bookshop2.exception.LogNotFoundException;
import com.example.bookshop2.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "Logs", description = "API для работы с логами")
public class LogController {
    private static final Logger LOG = LoggerFactory.getLogger(LogController.class);
    private static final String INVALID_DATE_LOG_MESSAGE = "Log message: {}";
    private static final String LOG_DIRECTORY = "logs"; // Папка с логами
    private final LogService logService;

    @Operation(summary = "Загрузить лог по дате", description = "Позволяет скачать лог-файл за указанную дату")
    @GetMapping("/{date}")
    public ResponseEntity<String> downloadLog(
            @Parameter(description = "Дата лог-файла в формате yyyy-MM-dd") @PathVariable String date) {
        String filename = String.format("bookshop2-%s.log", date);
        Path filePath = Paths.get(LOG_DIRECTORY).resolve(filename);

        try {
            String logContent = logService.getLogForDate(date); // Получаем отфильтрованные логи как строку
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(logContent);
        } catch (Exception e) {
            LOG.warn(INVALID_DATE_LOG_MESSAGE, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Получить статус генерации лога", description = "Возвращает статус обработки лога по ID")
    @GetMapping("/status/{id}")
    public ResponseEntity<String> getStatus(@PathVariable String id) {
        return ResponseEntity.ok(logService.getStatus(id).name());
    }

    @Operation(summary = "Скачать сгенерированный лог", description = "Скачивает объединенный лог по ID")
    @GetMapping("/file/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        try {
            byte[] content = logService.getLogFile(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"log_" + id + ".txt\"")
                    .body(content);
        } catch (LogNotFoundException e) {
            LOG.warn(INVALID_DATE_LOG_MESSAGE, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            LOG.warn(INVALID_DATE_LOG_MESSAGE, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Сгенерировать лог за период",
            description = "Запускает асинхронную генерацию лога за указанный период")
    @PostMapping("/generate/period")
    public ResponseEntity<String> generateLogForPeriod(
            @RequestParam String from,
            @RequestParam String to) {
        try {
            LocalDate start = LocalDate.parse(from);
            LocalDate end = LocalDate.parse(to);

            String id = logService.generateLogForPeriodAsync(start, end);
            return ResponseEntity.ok(id);
        } catch (DateTimeParseException e) {
            LOG.warn(INVALID_DATE_LOG_MESSAGE, e.getMessage());
            return ResponseEntity.badRequest().body("Неверный формат даты. Используйте yyyy-MM-dd");
        }
    }
}