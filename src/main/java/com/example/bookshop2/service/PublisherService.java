package com.example.bookshop2.service;

import com.example.bookshop2.dto.PublisherDto;
import com.example.bookshop2.exception.PublisherNotFoundException;
import com.example.bookshop2.exception.ValidationException;
import com.example.bookshop2.mapper.PublisherMapper;
import com.example.bookshop2.model.Publisher;
import com.example.bookshop2.repository.PublisherRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublisherService {
    private final PublisherRepository publisherRepository;
    private final CacheManager cacheManager;
    private static final String PUBLISHER_PREFIX = "publisher_";

    public PublisherService(PublisherRepository publisherRepository, CacheManager cacheManager) {
        this.publisherRepository = publisherRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public List<PublisherDto> findAll() {
        return publisherRepository.findAll().stream()
                .map(PublisherMapper::toDto)
                .toList();
    }

    @Transactional
    public PublisherDto findById(Long id) {
        String cacheKey = PUBLISHER_PREFIX + id;
        PublisherDto cachedPublisher = cacheManager.getFromCache(cacheKey, PublisherDto.class);
        if (cachedPublisher != null) {
            return cachedPublisher;
        }

        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException(id));
        PublisherDto publisherDto = PublisherMapper.toDto(publisher);
        cacheManager.saveToCache(cacheKey, publisherDto);
        return publisherDto;
    }

    @Transactional
    public PublisherDto findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Publisher name cannot be null or empty");
        }
        String cacheKey = "publisher_name_" + name.toLowerCase();
        PublisherDto cachedPublisher = cacheManager.getFromCache(cacheKey, PublisherDto.class);
        if (cachedPublisher != null) {
            return cachedPublisher;
        }

        Publisher publisher = publisherRepository.findByName(name)
                .orElseThrow(() -> new PublisherNotFoundException(name));
        PublisherDto publisherDto = PublisherMapper.toDto(publisher);
        cacheManager.saveToCache(cacheKey, publisherDto);
        return publisherDto;
    }

    @Transactional
    public List<PublisherDto> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Search name cannot be null or empty");
        }
        String cacheKey = "publisher_search_" + name.toLowerCase();
        @SuppressWarnings("unchecked")
        List<PublisherDto> cachedPublishers = cacheManager.getFromCache(cacheKey, List.class);
        if (cachedPublishers != null) {
            return cachedPublishers;
        }

        List<PublisherDto> publishers = publisherRepository.findByNameContainingIgnoreCase(name).stream()
                .map(PublisherMapper::toDto)
                .toList();
        cacheManager.saveToCache(cacheKey, publishers);
        return publishers;
    }

    @Transactional
    public PublisherDto create(PublisherDto dto) {
        Publisher publisher = PublisherMapper.fromDto(dto);
        Publisher savedPublisher = publisherRepository.save(publisher);
        cacheManager.clearPublisherCache(savedPublisher.getName());
        cacheManager.clearByPrefix(PUBLISHER_PREFIX + savedPublisher.getId());
        return PublisherMapper.toDto(savedPublisher);
    }

    @Transactional
    public void delete(Long id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException(id));
        String publisherName = publisher.getName();
        publisherRepository.deleteById(id);
        cacheManager.clearPublisherCache(publisherName);
        cacheManager.clearByPrefix(PUBLISHER_PREFIX + id);
    }

    @Transactional
    public PublisherDto update(Long id, PublisherDto dto) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new PublisherNotFoundException(id));
        String oldPublisherName = publisher.getName();

        PublisherMapper.updateFromDto(publisher, dto);
        Publisher savedPublisher = publisherRepository.save(publisher);
        cacheManager.clearPublisherCache(oldPublisherName);
        cacheManager.clearPublisherCache(dto.getName());
        cacheManager.clearByPrefix(PUBLISHER_PREFIX + id);
        return PublisherMapper.toDto(savedPublisher);
    }
}