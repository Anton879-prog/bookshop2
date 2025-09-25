package com.example.bookshop2.service;

import com.example.bookshop2.exception.LogNotFoundException;
import com.example.bookshop2.exception.LogReadException;
import com.example.bookshop2.exception.LogServiceInitializationException;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    private LogService logService;
    private FileSystem fileSystem;
    private Path logsDirectory;

    @Mock
    private Executor executor;

    private Field logStatusMapField;
    private Field logFilesField;
    private Field logsDirectoryField;

    @BeforeEach
    void setUp() {
        try {
            fileSystem = Jimfs.newFileSystem(Configuration.unix());
            logsDirectory = fileSystem.getPath("logs");
            Files.createDirectories(logsDirectory);

            logService = new LogService();

            logStatusMapField = LogService.class.getDeclaredField("logStatusMap");
            logStatusMapField.setAccessible(true);

            logFilesField = LogService.class.getDeclaredField("logFiles");
            logFilesField.setAccessible(true);

            logsDirectoryField = LogService.class.getDeclaredField("logsDirectory");
            logsDirectoryField.setAccessible(true);
            logsDirectoryField.set(logService, logsDirectory);

            Awaitility.setDefaultTimeout(10, TimeUnit.SECONDS);
        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            fail("Не удалось настроить тестовую среду", e);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (fileSystem != null) {
            fileSystem.close();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, LogService.LogStatus> getLogStatusMap() {
        try {
            return (ConcurrentHashMap<String, LogService.LogStatus>) logStatusMapField.get(logService);
        } catch (IllegalAccessException e) {
            fail("Не удалось получить logStatusMap", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Path> getLogFiles() {
        try {
            return (ConcurrentHashMap<String, Path>) logFilesField.get(logService);
        } catch (IllegalAccessException e) {
            fail("Не удалось получить logFiles", e);
            return null;
        }
    }

    @Test
    void generateLogForPeriodAsync_whenLogsExist_shouldGenerateLogFile() {
        LocalDate startDate = LocalDate.of(2025, 5, 1);
        LocalDate endDate = LocalDate.of(2025, 5, 2);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            Files.writeString(logsDirectory.resolve("bookshop2-2025-05-01.log"), "Log entry 1\n");
            Files.writeString(logsDirectory.resolve("bookshop2-2025-05-02.log"), "Log entry 2\n");
        } catch (IOException e) {
            fail("Не удалось создать тестовые файлы", e);
        }

        Field executorField = null;
        try {
            executorField = LogService.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executorField.set(logService, executor);

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            }).when(executor).execute(any(Runnable.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Не удалось настроить executor", e);
        }

        String id = logService.generateLogForPeriodAsync(startDate, endDate);

        Awaitility.await().until(() -> getLogStatusMap().get(id) == LogService.LogStatus.READY);

        assertThat(getLogStatusMap().get(id)).isEqualTo(LogService.LogStatus.READY);
        Path mergedLog = getLogFiles().get(id);
        assertThat(mergedLog).isNotNull();
        assertThat(mergedLog.getFileName().toString()).startsWith("log_period_");

        try {
            String content = Files.readString(mergedLog);
            assertThat(content).contains("===== bookshop2-2025-05-01.log =====", "Log entry 1");
            assertThat(content).contains("===== bookshop2-2025-05-02.log =====", "Log entry 2");
        } catch (IOException e) {
            fail("Не удалось прочитать объединённый лог", e);
        }
    }

    @Test
    void generateLogForPeriodAsync_whenNoLogsExist_shouldSetFailedStatus() {
        LocalDate startDate = LocalDate.of(2025, 5, 1);
        LocalDate endDate = LocalDate.of(2025, 5, 2);

        Field executorField = null;
        try {
            executorField = LogService.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executorField.set(logService, executor);

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            }).when(executor).execute(any(Runnable.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Не удалось настроить executor", e);
        }

        String id = logService.generateLogForPeriodAsync(startDate, endDate);

        Awaitility.await().until(() -> getLogStatusMap().get(id) == LogService.LogStatus.FAILED);

        assertThat(getLogStatusMap().get(id)).isEqualTo(LogService.LogStatus.FAILED);
        assertThat(getLogFiles().get(id)).isNull();
    }

    @Test
    void generateLogForPeriodAsync_whenIOException_shouldSetFailedStatus() {
        LocalDate startDate = LocalDate.of(2025, 5, 1);
        LocalDate endDate = LocalDate.of(2025, 5, 1);

        try {
            Files.deleteIfExists(logsDirectory);
            Files.createFile(logsDirectory); // Create file instead of dir to cause IO error
            logsDirectoryField.set(logService, logsDirectory);
        } catch (IOException | IllegalAccessException e) {
            fail("Не удалось настроить тестовую среду", e);
        }

        Field executorField = null;
        try {
            executorField = LogService.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executorField.set(logService, executor);

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            }).when(executor).execute(any(Runnable.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Не удалось настроить executor", e);
        }

        String id = logService.generateLogForPeriodAsync(startDate, endDate);

        Awaitility.await().until(() -> getLogStatusMap().get(id) == LogService.LogStatus.FAILED);

        assertThat(getLogStatusMap().get(id)).isEqualTo(LogService.LogStatus.FAILED);
        assertThat(getLogFiles().get(id)).isNull();
    }

    @Test
    void generateLogForPeriodAsync_whenInterrupted_shouldSetFailedStatus() {
        LocalDate startDate = LocalDate.of(2025, 5, 1);
        LocalDate endDate = LocalDate.of(2025, 5, 1);

        Field executorField = null;
        try {
            executorField = LogService.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executorField.set(logService, executor);

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                Thread.currentThread().interrupt(); // Set interrupt flag
                task.run(); // Run to trigger InterruptedException in sleep
                return null;
            }).when(executor).execute(any(Runnable.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Не удалось настроить executor", e);
        }

        String id = logService.generateLogForPeriodAsync(startDate, endDate);

        Awaitility.await().until(() -> getLogStatusMap().get(id) == LogService.LogStatus.FAILED);

        assertThat(getLogStatusMap().get(id)).isEqualTo(LogService.LogStatus.FAILED);
        assertThat(getLogFiles().get(id)).isNull();
    }

    @Test
    void getLogFile_whenReady_shouldReturnContent() {
        LocalDate startDate = LocalDate.of(2025, 5, 1);
        LocalDate endDate = LocalDate.of(2025, 5, 1);

        try {
            Files.writeString(logsDirectory.resolve("bookshop2-2025-05-01.log"), "Test log\n");
        } catch (IOException e) {
            fail("Не удалось создать тестовый файл", e);
        }

        Field executorField = null;
        try {
            executorField = LogService.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executorField.set(logService, executor);

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            }).when(executor).execute(any(Runnable.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Не удалось настроить executor", e);
        }

        String id = logService.generateLogForPeriodAsync(startDate, endDate);
        Awaitility.await().until(() -> getLogStatusMap().get(id) == LogService.LogStatus.READY);

        byte[] content = logService.getLogFile(id);
        assertThat(content).isNotEmpty();
        assertThat(new String(content)).contains("Test log");
    }

    @Test
    void getLogFile_whenNotReady_shouldThrowLogNotFoundException() {
        String id = "nonexistent-id";
        assertThrows(LogNotFoundException.class, () -> logService.getLogFile(id));
    }

    @Test
    void getLogForDate_whenFileExistsAndLogsPresent_shouldReturnFilteredLogs() {
        Path logFile = logsDirectory.resolve("bookshop2-all.log");
        try {
            Files.writeString(logFile, "2025-05-01 Log entry 1\n2025-05-02 Log entry 2\n");
        } catch (IOException e) {
            fail("Не удалось создать тестовый файл", e);
        }

        String result = logService.getLogForDate("2025-05-01");
        assertThat(result).contains("2025-05-01 Log entry 1");
        assertThat(result).doesNotContain("2025-05-02");
    }

    @Test
    void getLogForDate_whenFileExistsButNoLogsForDate_shouldThrowLogNotFoundException() {
        Path logFile = logsDirectory.resolve("bookshop2-all.log");
        try {
            Files.writeString(logFile, "2025-05-02 Log entry 2\n");
        } catch (IOException e) {
            fail("Не удалось создать тестовый файл", e);
        }

        assertThrows(LogNotFoundException.class, () -> logService.getLogForDate("2025-05-01"));
    }

    @Test
    void getLogForDate_whenFileDoesNotExist_shouldThrowLogNotFoundException() {
        assertThrows(LogNotFoundException.class, () -> logService.getLogForDate("2025-05-01"));
    }

    @Test
    void getLogForDate_whenIOException_shouldThrowLogReadException() {
        Path logFile = logsDirectory.resolve("bookshop2-all.log");
        try {
            Files.createDirectory(logFile); // Create directory to cause IOException on Files.lines
        } catch (IOException e) {
            fail("Не удалось настроить тестовую среду", e);
        }

        assertThrows(LogReadException.class, () -> logService.getLogForDate("2025-05-01"));
    }

//    @Test
//    void getStatus_whenInProgress_shouldReturnInProgress() {
//        LocalDate startDate = LocalDate.of(2025, 5, 1);
//        LocalDate endDate = LocalDate.of(2025, 5, 1);
//
//        Field executorField = null;
//        try {
//            executorField = LogService.class.getDeclaredField("executor");
//            executorField.setAccessible(true);
//            executorField.set(logService, executor);
//
//            doAnswer(invocation -> {
//                Runnable task = invocation.getArgument(0);
//                // Delay to ensure IN_PROGRESS is set before running
//                task.run();
//                return null;
//            }).when(executor).execute(any(Runnable.class));
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            fail("Не удалось настроить executor", e);
//        }
//
//        String id = logService.generateLogForPeriodAsync(startDate, endDate);
//        // Check status immediately after call, before task completes
//        LogService.LogStatus status = logService.getStatus(id);
//        assertThat(status).isEqualTo(LogService.LogStatus.IN_PROGRESS);
//    }

    @Test
    void getStatus_whenNotFound_shouldReturnNotFound() {
        assertThat(logService.getStatus("nonexistent-id")).isEqualTo(LogService.LogStatus.NOT_FOUND);
    }

    @Test
    void constructor_whenValid_shouldNotThrow() {
        assertDoesNotThrow(() -> new LogService());
    }
}
