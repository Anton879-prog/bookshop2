package com.example.bookshop2.service;

import com.example.bookshop2.dto.AuthorDto;
import com.example.bookshop2.exception.AuthorNotFoundException;
import com.example.bookshop2.exception.ValidationException;
import com.example.bookshop2.model.Author;
import com.example.bookshop2.repository.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private AuthorService authorService;

    private Author author;
    private AuthorDto authorDto;

    // Заглушка для AuthorMapper
    static class AuthorMapper {
        private AuthorMapper() {}

        static AuthorDto toDto(Author author) {
            AuthorDto dto = new AuthorDto();
            dto.setId(author.getId());
            dto.setName(author.getName());
            return dto;
        }

        static Author fromDto(AuthorDto dto) {
            Author author = new Author();
            author.setName(dto.getName());
            return author;
        }

        static void updateFromDto(Author author, AuthorDto dto) {
            if (dto.getName() != null) {
                author.setName(dto.getName());
            }
        }
    }

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setName("John Doe");

        authorDto = new AuthorDto();
        authorDto.setId(1L);
        authorDto.setName("John Doe");
    }

    @Test
    void findAll_shouldReturnListOfAuthors() {
        when(authorRepository.findAll()).thenReturn(List.of(author));

        List<AuthorDto> result = authorService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        verify(authorRepository).findAll();
    }

    @Test
    void findById_whenCached_shouldReturnFromCache() {
        when(cacheManager.getFromCache("author_1", AuthorDto.class)).thenReturn(authorDto);

        AuthorDto result = authorService.findById(1L);

        assertThat(result.getName()).isEqualTo("John Doe");
        verify(authorRepository, never()).findById(any());
        verify(cacheManager).getFromCache("author_1", AuthorDto.class);
    }

    @Test
    void findById_whenNotCached_shouldReturnFromDB() {
        when(cacheManager.getFromCache("author_1", AuthorDto.class)).thenReturn(null);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        AuthorDto result = authorService.findById(1L);

        assertThat(result.getName()).isEqualTo("John Doe");
        verify(authorRepository).findById(1L);
        verify(cacheManager).saveToCache("author_1", result);
    }

    @Test
    void findById_whenNotFound_shouldThrowAuthorNotFoundException() {
        when(cacheManager.getFromCache("author_99", AuthorDto.class)).thenReturn(null);
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AuthorNotFoundException.class, () -> authorService.findById(99L));
        verify(authorRepository).findById(99L);
    }

    @Test
    void searchByName_whenValidName_shouldReturnAuthors() {
        when(authorRepository.findByNameContainingIgnoreCase("John")).thenReturn(List.of(author));

        List<AuthorDto> result = authorService.searchByName("John");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        verify(cacheManager).saveToCache("author_search_john", result);
        verify(authorRepository).findByNameContainingIgnoreCase("John");
    }

    @Test
    void searchByName_whenCached_shouldReturnFromCache() {
        List<AuthorDto> cachedList = List.of(authorDto);
        when(cacheManager.getFromCache("author_search_john", List.class)).thenReturn(cachedList);

        List<AuthorDto> result = authorService.searchByName("John");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        verify(authorRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    void searchByName_whenEmptyName_shouldThrowValidationException() {
        assertThrows(ValidationException.class, () -> authorService.searchByName(""));
        assertThrows(ValidationException.class, () -> authorService.searchByName(null));
        verify(authorRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    void create_whenValid_shouldCreateAuthor() {
        when(authorRepository.save(any(Author.class))).thenReturn(author);

        AuthorDto result = authorService.create(authorDto);

        assertThat(result.getName()).isEqualTo("John Doe");
        verify(authorRepository).save(any(Author.class));
        verify(cacheManager).clearAuthorCache(1L);
    }

    @Test
    void delete_whenExists_shouldDelete() {
        when(authorRepository.existsById(1L)).thenReturn(true);

        authorService.delete(1L);

        verify(authorRepository).deleteById(1L);
        verify(cacheManager).clearAuthorCache(1L);
    }

    @Test
    void delete_whenNotExists_shouldThrowAuthorNotFoundException() {
        when(authorRepository.existsById(99L)).thenReturn(false);

        assertThrows(AuthorNotFoundException.class, () -> authorService.delete(99L));
        verify(authorRepository, never()).deleteById(any());
    }

    @Test
    void update_whenValid_shouldUpdateAuthor() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.save(author)).thenReturn(author);

        AuthorDto result = authorService.update(1L, authorDto);

        assertThat(result.getName()).isEqualTo("John Doe");
        verify(authorRepository).save(author);
        verify(cacheManager).clearAuthorCache(1L);
    }

    @Test
    void update_whenNotFound_shouldThrowAuthorNotFoundException() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AuthorNotFoundException.class, () -> authorService.update(99L, authorDto));
        verify(authorRepository, never()).save(any());
    }
}