package com.example.bookshop2.service;

import com.example.bookshop2.exception.LogNotFoundException;
import com.example.bookshop2.exception.LogReadException;
import com.example.bookshop2.exception.LogServiceInitializationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogService {
    private final Map<String, LogStatus> logStatusMap = new ConcurrentHashMap<>();
    private final Map<String, Path> logFiles = new ConcurrentHashMap<>();
    private final Executor executor = Executors.newCachedThreadPool();
    private final Path logsDirectory = Paths.get("logs");

    public LogService() {
        try {
            Files.createDirectories(logsDirectory);
        } catch (IOException e) {
            log.error("Не удалось создать папку logs", e);
            throw new LogServiceInitializationException("Ошибка при инициализации LogService", e);
        }
    }

    public String generateLogForPeriodAsync(LocalDate startDate, LocalDate endDate) {
        String id = UUID.randomUUID().toString();
        logStatusMap.put(id, LogStatus.IN_PROGRESS);

        executor.execute(() -> {
            try {
                Thread.sleep(20_000);
                StringBuilder collectedLogs = new StringBuilder();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                LocalDate current = startDate;
                while (!current.isAfter(endDate)) {
                    String fileName = "bookshop2-" + current.format(formatter) + ".log";
                    Path filePath = logsDirectory.resolve(fileName);

                    if (Files.exists(filePath)) {
                        collectedLogs.append("===== ").append(fileName).append(" =====\n");
                        collectedLogs.append(Files.readString(filePath)).append("\n");
                    }

                    current = current.plusDays(1);
                }

                if (collectedLogs.isEmpty()) {
                    logStatusMap.put(id, LogStatus.FAILED);
                    return;
                }

                Path mergedLog = logsDirectory.resolve("log_period_" + id + ".log");
                Files.writeString(mergedLog, collectedLogs.toString());
                logFiles.put(id, mergedLog);
                logStatusMap.put(id, LogStatus.READY);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logStatusMap.put(id, LogStatus.FAILED);
                log.error("Генерация периодического лог-файла была прервана", ie);
            } catch (IOException e) {
                logStatusMap.put(id, LogStatus.FAILED);
                log.error("Ошибка при сборе логов за период", e);
            }
        });

        return id;
    }

    public byte[] getLogFile(String id) {
        if (logStatusMap.get(id) == LogStatus.READY) {
            try {
                return Files.readAllBytes(logFiles.get(id));
            } catch (IOException e) {
                throw new LogReadException("Ошибка чтения файла лога", e);
            }
        }
        throw new LogNotFoundException("Файл ещё не готов или не существует");
    }

    public String getLogForDate(String date) {
        Path filePath = logsDirectory.resolve("bookshop2-all.log"); // Общий файл для всех дат
        try {
            if (!Files.exists(filePath)) {
                throw new LogNotFoundException("Общий лог-файл bookshop2-all.log не найден");
            }
            StringBuilder filteredLogs = new StringBuilder();
            Files.lines(filePath)
                    .filter(line -> line.startsWith(date)) // Фильтруем строки по дате (yyyy-MM-dd)
                    .forEach(line -> filteredLogs.append(line).append("\n"));
            if (filteredLogs.length() == 0) {
                throw new LogNotFoundException("Логи за дату " + date + " не найдены в общем файле");
            }
            return filteredLogs.toString();
        } catch (IOException e) {
            throw new LogReadException("Ошибка чтения общего лог-файла для даты " + date, e);
        }
    }

    public LogStatus getStatus(String id) {
        return logStatusMap.getOrDefault(id, LogStatus.NOT_FOUND);
    }

    public enum LogStatus {
        IN_PROGRESS, READY, FAILED, NOT_FOUND
    }
}