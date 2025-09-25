package com.example.bookshop2.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class VisitCounterService {
    private final Map<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();

    public void increment(String path) {
        counterMap.computeIfAbsent(path, key -> new AtomicInteger(0)).incrementAndGet();
    }

    public Map<String, Integer> getAllCounts() {
        return counterMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }
}