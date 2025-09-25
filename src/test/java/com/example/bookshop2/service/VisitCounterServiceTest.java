package com.example.bookshop2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisitCounterServiceTest {

    private VisitCounterService visitCounterService;

    @BeforeEach
    void setUp() {
        visitCounterService = new VisitCounterService();
    }

    @Test
    void increment_whenNewPath_shouldCreateCounterWithValueOne() {
        // Arrange
        String path = "/home";

        // Act
        visitCounterService.increment(path);

        // Assert
        Map<String, Integer> counts = visitCounterService.getAllCounts();
        assertThat(counts).hasSize(1);
        assertThat(counts.get(path)).isEqualTo(1);
    }

    @Test
    void increment_whenExistingPath_shouldIncrementCounter() {
        // Arrange
        String path = "/home";
        visitCounterService.increment(path); // Первый вызов

        // Act
        visitCounterService.increment(path); // Второй вызов

        // Assert
        Map<String, Integer> counts = visitCounterService.getAllCounts();
        assertThat(counts).hasSize(1);
        assertThat(counts.get(path)).isEqualTo(2);
    }

    @Test
    void increment_whenMultiplePaths_shouldTrackEachSeparately() {
        // Arrange
        String path1 = "/home";
        String path2 = "/about";

        // Act
        visitCounterService.increment(path1);
        visitCounterService.increment(path1);
        visitCounterService.increment(path2);

        // Assert
        Map<String, Integer> counts = visitCounterService.getAllCounts();
        assertThat(counts).hasSize(2);
        assertThat(counts.get(path1)).isEqualTo(2);
        assertThat(counts.get(path2)).isEqualTo(1);
    }

    @Test
    void increment_whenPathIsNull_shouldThrowNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> visitCounterService.increment(null));
    }

    @Test
    void getAllCounts_whenNoCounts_shouldReturnEmptyMap() {
        // Act
        Map<String, Integer> counts = visitCounterService.getAllCounts();

        // Assert
        assertThat(counts).isEmpty();
    }

    @Test
    void getAllCounts_whenCountsExist_shouldReturnCorrectCounts() {
        // Arrange
        visitCounterService.increment("/home");
        visitCounterService.increment("/home");
        visitCounterService.increment("/about");

        // Act
        Map<String, Integer> counts = visitCounterService.getAllCounts();

        // Assert
        assertThat(counts).hasSize(2);
        assertThat(counts.get("/home")).isEqualTo(2);
        assertThat(counts.get("/about")).isEqualTo(1);
    }

    @Test
    void getAllCounts_whenModifiedExternally_shouldNotAffectInternalState() {
        // Arrange
        visitCounterService.increment("/home");

        // Act
        Map<String, Integer> counts = visitCounterService.getAllCounts();
        counts.put("/home", 999); // Пытаемся изменить возвращённую карту

        // Assert
        Map<String, Integer> newCounts = visitCounterService.getAllCounts();
        assertThat(newCounts.get("/home")).isEqualTo(1); // Внутреннее состояние не изменилось
    }
}