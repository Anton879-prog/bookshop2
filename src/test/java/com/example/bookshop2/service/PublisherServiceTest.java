package com.example.bookshop2.service;

import com.example.bookshop2.dto.PublisherDto;
import com.example.bookshop2.exception.PublisherNotFoundException;
import com.example.bookshop2.exception.ValidationException;
import com.example.bookshop2.mapper.PublisherMapper;
import com.example.bookshop2.model.Publisher;
import com.example.bookshop2.repository.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublisherServiceTest {

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private PublisherService publisherService;

    private Publisher publisher;
    private PublisherDto publisherDto;

    @BeforeEach
    void setUp() {
        publisher = new Publisher();
        publisher.setId(1L);
        publisher.setName("Acme Publishing");

        publisherDto = new PublisherDto();
        publisherDto.setId(1L);
        publisherDto.setName("Acme Publishing");
    }

    @Test
    void findAll_shouldReturnAllPublishers() {
        try (MockedStatic<PublisherMapper> mockedStatic = mockStatic(PublisherMapper.class)) {
            // Arrange
            when(publisherRepository.findAll()).thenReturn(List.of(publisher));
            mockedStatic.when(() -> PublisherMapper.toDto(publisher)).thenReturn(publisherDto);

            // Act
            List<PublisherDto> result = publisherService.findAll();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(publisherDto);
            verify(publisherRepository).findAll();
            mockedStatic.verify(() -> PublisherMapper.toDto(publisher));
        }
    }

    @Test
    void findById_whenCached_shouldReturnCachedPublisher() {
        // Arrange
        String cacheKey = "publisher_1";
        when(cacheManager.getFromCache(cacheKey, PublisherDto.class)).thenReturn(publisherDto);

        // Act
        PublisherDto result = publisherService.findById(1L);

        // Assert
        assertThat(result).isEqualTo(publisherDto);
        verify(cacheManager).getFromCache(cacheKey, PublisherDto.class);
        verifyNoInteractions(publisherRepository);
    }

    @Test
    void findById_whenNotCachedAndExists_shouldReturnPublisherAndCacheIt() {
        try (MockedStatic<PublisherMapper> mockedStatic = mockStatic(PublisherMapper.class)) {
            // Arrange
            String cacheKey = "publisher_1";
            when(cacheManager.getFromCache(cacheKey, PublisherDto.class)).thenReturn(null);
            when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
            mockedStatic.when(() -> PublisherMapper.toDto(publisher)).thenReturn(publisherDto);

            // Act
            PublisherDto result = publisherService.findById(1L);

            // Assert
            assertThat(result).isEqualTo(publisherDto);
            verify(cacheManager).getFromCache(cacheKey, PublisherDto.class);
            verify(publisherRepository).findById(1L);
            mockedStatic.verify(() -> PublisherMapper.toDto(publisher));
            verify(cacheManager).saveToCache(cacheKey, publisherDto);
        }
    }

    @Test
    void findById_whenNotFound_shouldThrowPublisherNotFoundException() {
        // Arrange
        String cacheKey = "publisher_1";
        when(cacheManager.getFromCache(cacheKey, PublisherDto.class)).thenReturn(null);
        when(publisherRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        PublisherNotFoundException exception = assertThrows(PublisherNotFoundException.class,
                () -> publisherService.findById(1L));
        assertThat(exception.getMessage()).contains("1");
        verify(cacheManager).getFromCache(cacheKey, PublisherDto.class);
        verify(publisherRepository).findById(1L);
        verifyNoMoreInteractions(cacheManager);
    }

    @Test
    void findByName_whenCached_shouldReturnCachedPublisher() {
        // Arrange
        String name = "Acme Publishing";
        String cacheKey = "publisher_name_acme publishing";
        when(cacheManager.getFromCache(cacheKey, PublisherDto.class)).thenReturn(publisherDto);

        // Act
        PublisherDto result = publisherService.findByName(name);

        // Assert
        assertThat(result).isEqualTo(publisherDto);
        verify(cacheManager).getFromCache(cacheKey, PublisherDto.class);
        verifyNoInteractions(publisherRepository);
    }

    @Test
    void findByName_whenNotCachedAndExists_shouldReturnPublisherAndCacheIt() {
        try (MockedStatic<PublisherMapper> mockedStatic = mockStatic(PublisherMapper.class)) {
            // Arrange
            String name = "Acme Publishing";
            String cacheKey = "publisher_name_acme publishing";
            when(cacheManager.getFromCache(cacheKey, PublisherDto.class)).thenReturn(null);
            when(publisherRepository.findByName(name)).thenReturn(Optional.of(publisher));
            mockedStatic.when(() -> PublisherMapper.toDto(publisher)).thenReturn(publisherDto);

            // Act
            PublisherDto result = publisherService.findByName(name);

            // Assert
            assertThat(result).isEqualTo(publisherDto);
            verify(cacheManager).getFromCache(cacheKey, PublisherDto.class);
            verify(publisherRepository).findByName(name);
            mockedStatic.verify(() -> PublisherMapper.toDto(publisher));
            verify(cacheManager).saveToCache(cacheKey, publisherDto);
        }
    }

    @Test
    void findByName_whenNotFound_shouldThrowPublisherNotFoundException() {
        // Arrange
        String name = "Nonexistent";
        String cacheKey = "publisher_name_nonexistent";
        when(cacheManager.getFromCache(cacheKey, PublisherDto.class)).thenReturn(null);
        when(publisherRepository.findByName(name)).thenReturn(Optional.empty());

        // Act & Assert
        PublisherNotFoundException exception = assertThrows(PublisherNotFoundException.class,
                () -> publisherService.findByName(name));
        assertThat(exception.getMessage()).contains("Nonexistent");
        verify(cacheManager).getFromCache(cacheKey, PublisherDto.class);
        verify(publisherRepository).findByName(name);
        verifyNoMoreInteractions(cacheManager);
    }

    @Test
    void findByName_whenNameIsEmpty_shouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> publisherService.findByName(""));
        assertThat(exception.getMessage()).contains("Publisher name cannot be null or empty");
        verifyNoInteractions(cacheManager, publisherRepository);
    }

    @Test
    void searchByName_whenCached_shouldReturnCachedPublishers() {
        // Arrange
        String name = "Acme";
        String cacheKey = "publisher_search_acme";
        List<PublisherDto> cachedPublishers = List.of(publisherDto);
        when(cacheManager.getFromCache(cacheKey, List.class)).thenReturn(cachedPublishers);

        // Act
        List<PublisherDto> result = publisherService.searchByName(name);

        // Assert
        assertThat(result).isEqualTo(cachedPublishers);
        verify(cacheManager).getFromCache(cacheKey, List.class);
        verifyNoInteractions(publisherRepository);
    }

    @Test
    void searchByName_whenNotCachedAndExists_shouldReturnPublishersAndCacheThem() {
        try (MockedStatic<PublisherMapper> mockedStatic = mockStatic(PublisherMapper.class)) {
            // Arrange
            String name = "Acme";
            String cacheKey = "publisher_search_acme";
            when(cacheManager.getFromCache(cacheKey, List.class)).thenReturn(null);
            when(publisherRepository.findByNameContainingIgnoreCase(name)).thenReturn(List.of(publisher));
            mockedStatic.when(() -> PublisherMapper.toDto(publisher)).thenReturn(publisherDto);

            // Act
            List<PublisherDto> result = publisherService.searchByName(name);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(publisherDto);
            verify(cacheManager).getFromCache(cacheKey, List.class);
            verify(publisherRepository).findByNameContainingIgnoreCase(name);
            mockedStatic.verify(() -> PublisherMapper.toDto(publisher));
            verify(cacheManager).saveToCache(cacheKey, result);
        }
    }

    @Test
    void searchByName_whenNameIsNull_shouldThrowValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> publisherService.searchByName(null));
        assertThat(exception.getMessage()).contains("Search name cannot be null or empty");
        verifyNoInteractions(cacheManager, publisherRepository);
    }

    @Test
    void create_shouldSavePublisherAndClearCache() {
        try (MockedStatic<PublisherMapper> mockedStatic = mockStatic(PublisherMapper.class)) {
            // Arrange
            mockedStatic.when(() -> PublisherMapper.fromDto(publisherDto)).thenReturn(publisher);
            when(publisherRepository.save(publisher)).thenReturn(publisher);
            mockedStatic.when(() -> PublisherMapper.toDto(publisher)).thenReturn(publisherDto);

            // Act
            PublisherDto result = publisherService.create(publisherDto);

            // Assert
            assertThat(result).isEqualTo(publisherDto);
            mockedStatic.verify(() -> PublisherMapper.fromDto(publisherDto));
            verify(publisherRepository).save(publisher);
            mockedStatic.verify(() -> PublisherMapper.toDto(publisher));
            verify(cacheManager).clearPublisherCache("Acme Publishing");
            verify(cacheManager).clearByPrefix("publisher_1");
        }
    }

    @Test
    void delete_whenPublisherExists_shouldDeleteAndClearCache() {
        // Arrange
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));

        // Act
        publisherService.delete(1L);

        // Assert
        verify(publisherRepository).findById(1L);
        verify(publisherRepository).deleteById(1L);
        verify(cacheManager).clearPublisherCache("Acme Publishing");
        verify(cacheManager).clearByPrefix("publisher_1");
    }

    @Test
    void delete_whenPublisherNotFound_shouldThrowPublisherNotFoundException() {
        // Arrange
        when(publisherRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        PublisherNotFoundException exception = assertThrows(PublisherNotFoundException.class,
                () -> publisherService.delete(1L));
        assertThat(exception.getMessage()).contains("1");
        verify(publisherRepository).findById(1L);
        verifyNoMoreInteractions(publisherRepository, cacheManager);
    }

    @Test
    void update_whenPublisherExists_shouldUpdateAndClearCache() {
        try (MockedStatic<PublisherMapper> mockedStatic = mockStatic(PublisherMapper.class)) {
            // Arrange
            PublisherDto updatedDto = new PublisherDto();
            updatedDto.setId(1L);
            updatedDto.setName("Updated Publisher");

            when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
            when(publisherRepository.save(publisher)).thenReturn(publisher);
            mockedStatic.when(() -> PublisherMapper.toDto(publisher)).thenReturn(publisherDto);

            // Act
            PublisherDto result = publisherService.update(1L, updatedDto);

            // Assert
            assertThat(result).isEqualTo(publisherDto);
            verify(publisherRepository).findById(1L);
            mockedStatic.verify(() -> PublisherMapper.updateFromDto(publisher, updatedDto));
            verify(publisherRepository).save(publisher);
            mockedStatic.verify(() -> PublisherMapper.toDto(publisher));
            verify(cacheManager).clearPublisherCache("Acme Publishing");
            verify(cacheManager).clearPublisherCache("Updated Publisher");
            verify(cacheManager).clearByPrefix("publisher_1");
        }
    }

    @Test
    void update_whenPublisherNotFound_shouldThrowPublisherNotFoundException() {
        // Arrange
        when(publisherRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        PublisherNotFoundException exception = assertThrows(PublisherNotFoundException.class,
                () -> publisherService.update(1L, publisherDto));
        assertThat(exception.getMessage()).contains("1");
        verify(publisherRepository).findById(1L);
        verifyNoMoreInteractions(publisherRepository, cacheManager);
    }
}