package com.example.bookshop2.service;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);
    private static final int MAX_CACHE_SIZE = 5;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public CacheManager() {
        startCacheCleanupTask();
    }

    public void saveToCache(String key, Object value) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            removeOldestEntry();
        }
        cache.put(key, new CacheEntry(value));
        LOGGER.info("💾 Данные сохранены в кэш: {}", key); // NOSONAR
    }

    public <T> T getFromCache(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            LOGGER.info("✅ Данные взяты из кэша: {}", key); // NOSONAR
            return type.cast(entry.getValue());
        }
        cache.remove(key);
        LOGGER.info("❌ Кэш не найден или устарел для ключа: {}", key); // NOSONAR
        return null;
    }

    public void clearPublisherCache(String publisherName) {
        String key = "publishers_" + publisherName;
        cache.remove(key);
        LOGGER.info("🗑 Кэш очищен для издателя: {}", publisherName); // NOSONAR
    }

    public void clearBookCache(Long bookId) {
        String key = "book_" + bookId;
        cache.remove(key);
        LOGGER.info("🗑 Кэш очищен для книги с ID: {}", bookId); // NOSONAR
    }

    public void clearAuthorCache(Long authorId) {
        String key = "author_" + authorId;
        cache.remove(key);
        LOGGER.info("🗑 Кэш очищен для автора с ID: {}", authorId); // NOSONAR
    }

    public void clearByPrefix(String prefix) {
        cache.keySet().removeIf(key -> key.startsWith(prefix));
        LOGGER.info("🗑 Очищен кэш для префикса: {}", prefix); // NOSONAR
    }

    private void removeOldestEntry() {
        Optional<Map.Entry<String, CacheEntry>> oldestEntry = cache.entrySet()
                .stream()
                .min(Comparator.comparing(e -> e.getValue().getTimestamp()));
        oldestEntry.ifPresent(entry -> {
            cache.remove(entry.getKey());
            LOGGER.info("🗑 Удалён самый старый кэш-ключ: {}", entry.getKey()); // NOSONAR
        });
    }

    @Scheduled(fixedRate = 10_000)
    public void startCacheCleanupTask() {
        try {
            LOGGER.info("🔍 Кэш до очистки: {}", cache.keySet());
            cache.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().isExpired();
                if (expired) {
                    LOGGER.info("🗑 Удаляется ключ: {}", entry.getKey()); // NOSONAR
                }
                return expired;
            });
            LOGGER.info("🧹 Очистка устаревших записей из кэша, текущий размер: {}, ключи: {}",
                    cache.size(), cache.keySet()); // NOSONAR
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка очистки кэша", e);
        }
    }
}