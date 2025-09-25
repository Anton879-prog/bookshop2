package com.example.bookshop2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CacheManagerTest {

    private CacheManager cacheManager;
    private Field cacheField;

    // Подкласс для управления временем
    private static class TestCacheEntry extends CacheEntry {
        private final Instant customTimestamp;

        TestCacheEntry(Object value, Instant timestamp) {
            super(value);
            this.customTimestamp = timestamp;
        }

        @Override
        public Instant getTimestamp() {
            return customTimestamp;
        }

        @Override
        public boolean isExpired() {
            return Instant.now().toEpochMilli() - customTimestamp.toEpochMilli() > 10_000;
        }
    }

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        cacheManager = new CacheManager();
        // Получаем доступ к приватному полю cache через рефлексию
        cacheField = CacheManager.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
    }

    // Вспомогательный метод для доступа к cache
    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, CacheEntry> getCache() throws IllegalAccessException {
        return (ConcurrentHashMap<String, CacheEntry>) cacheField.get(cacheManager);
    }

    @Test
    void testSaveAndGetFromCache() {
        String key = "testKey";
        String value = "testValue";

        cacheManager.saveToCache(key, value);
        String cachedValue = cacheManager.getFromCache(key, String.class);

        assertNotNull(cachedValue, "Значение должно быть получено из кэша");
        assertEquals(value, cachedValue, "Полученное значение не соответствует ожидаемому");
    }

    @Test
    void testCacheSizeLimit() throws IllegalAccessException {
        for (int i = 1; i <= 6; i++) {
            cacheManager.saveToCache("key" + i, "value" + i);
        }

        assertEquals(5, getCache().size(), "Размер кэша должен быть ограничен 5");
    }

    @Test
    void testClearPublisherCache() throws IllegalAccessException {
        String key = "publishers_Acme";
        getCache().put(key, new TestCacheEntry("data", Instant.now()));

        cacheManager.clearPublisherCache("Acme");
        assertNull(cacheManager.getFromCache(key, String.class), "Кэш должен быть очищен");
    }

    @Test
    void testClearBookCache() throws IllegalAccessException {
        String key = "book_1";
        getCache().put(key, new TestCacheEntry("data", Instant.now()));

        cacheManager.clearBookCache(1L);
        assertNull(cacheManager.getFromCache(key, String.class), "Кэш должен быть очищен");
    }

    @Test
    void testClearAuthorCache() throws IllegalAccessException {
        String key = "author_1";
        getCache().put(key, new TestCacheEntry("data", Instant.now()));

        cacheManager.clearAuthorCache(1L);
        assertNull(cacheManager.getFromCache(key, String.class), "Кэш должен быть очищен");
    }

    @Test
    void testClearByPrefix() throws IllegalAccessException {
        getCache().put("prefix_key1", new TestCacheEntry("data1", Instant.now()));
        getCache().put("prefix_key2", new TestCacheEntry("data2", Instant.now()));
        getCache().put("other_key", new TestCacheEntry("data3", Instant.now()));

        cacheManager.clearByPrefix("prefix_");

        assertNull(cacheManager.getFromCache("prefix_key1", String.class), "Кэш должен быть очищен");
        assertNull(cacheManager.getFromCache("prefix_key2", String.class), "Кэш должен быть очищен");
        assertNotNull(cacheManager.getFromCache("other_key", String.class), "Кэш для других ключей должен остаться");
    }

    @Test
    void testRemoveOldestEntryThroughCacheSave() throws IllegalAccessException {
        Instant baseTime = Instant.now().minusSeconds(10);
        getCache().put("key1", new TestCacheEntry("value1", baseTime));
        getCache().put("key2", new TestCacheEntry("value2", baseTime.plusMillis(100)));
        getCache().put("key3", new TestCacheEntry("value3", baseTime.plusMillis(200)));
        getCache().put("key4", new TestCacheEntry("value4", baseTime.plusMillis(300)));
        getCache().put("key5", new TestCacheEntry("value5", baseTime.plusMillis(400)));

        cacheManager.saveToCache("key6", "value6");

        assertNull(cacheManager.getFromCache("key1", String.class), "Самая старая запись должна быть удалена");
        assertNotNull(cacheManager.getFromCache("key6", String.class), "Новая запись должна быть добавлена");
    }

    @Test
    void testGetFromCacheWhenExpired() throws IllegalAccessException {
        String key = "key1";
        getCache().put(key, new TestCacheEntry("value1", Instant.now().minusSeconds(20)));

        String result = cacheManager.getFromCache(key, String.class);

        assertNull(result, "Значение должно быть null для устаревшего кэша");
        assertFalse(getCache().containsKey(key), "Устаревший ключ должен быть удалён");
    }

    @Test
    void testGetFromCacheWhenNotExists() {
        String result = cacheManager.getFromCache("nonexistent", String.class);

        assertNull(result, "Значение должно быть null для несуществующего ключа");
    }

    @Test
    void testCacheCleanupTask() throws IllegalAccessException {
        getCache().put("key1", new TestCacheEntry("value1", Instant.now().minusSeconds(20)));
        getCache().put("key2", new TestCacheEntry("value2", Instant.now()));

        cacheManager.startCacheCleanupTask();

        assertNull(cacheManager.getFromCache("key1", String.class), "Устаревший ключ должен быть удалён");
        assertNotNull(cacheManager.getFromCache("key2", String.class), "Свежий ключ должен остаться");
    }

    @Test
    void testCacheCleanupTaskWithException() throws IllegalAccessException {
        getCache().put("key1", new TestCacheEntry("value1", null) {
            @Override
            public boolean isExpired() {
                throw new RuntimeException("Test exception");
            }
        });

        cacheManager.startCacheCleanupTask();

        assertTrue(getCache().containsKey("key1"), "Ключ должен остаться, несмотря на исключение");
    }
}