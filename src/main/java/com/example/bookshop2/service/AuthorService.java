/*
package com.example.bookshop2.service;

import com.example.bookshop2.dto.AuthorDto;
import com.example.bookshop2.exception.AuthorNotFoundException;
import com.example.bookshop2.mapper.AuthorMapper;
import com.example.bookshop2.model.Author;
import com.example.bookshop2.repository.AuthorRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final CacheManager cacheManager;

    public AuthorService(AuthorRepository authorRepository, CacheManager cacheManager) {
        this.authorRepository = authorRepository;
        this.cacheManager = cacheManager;
    }

    public List<AuthorDto> findAll() {
        return authorRepository.findAll().stream()
                .map(AuthorMapper::toDto)
                .toList();
    }

    public AuthorDto findById(Long id) {
        String cacheKey = "author_" + id;
        AuthorDto cachedAuthor = cacheManager.getFromCache(cacheKey, AuthorDto.class);
        if (cachedAuthor != null) {
            return cachedAuthor;
        }

        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
        AuthorDto authorDto = AuthorMapper.toDto(author);
        cacheManager.saveToCache(cacheKey, authorDto);
        return authorDto;
    }

    public List<AuthorDto> searchByName(String name) {
        String cacheKey = "author_search_" + name.toLowerCase();
        @SuppressWarnings("unchecked")
        List<AuthorDto> cachedAuthors = cacheManager.getFromCache(cacheKey, List.class);
        if (cachedAuthors != null) {
            return cachedAuthors;
        }

        List<AuthorDto> authors = authorRepository.findByNameContainingIgnoreCase(name).stream()
                .map(AuthorMapper::toDto)
                .toList();
        cacheManager.saveToCache(cacheKey, authors);
        return authors;
    }

    @Transactional
    public AuthorDto create(AuthorDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Author name cannot be null or empty");
        }

        Author author = AuthorMapper.fromDto(dto);
        Author savedAuthor = authorRepository.save(author);
        cacheManager.clearAuthorCache(savedAuthor.getId());
        return AuthorMapper.toDto(savedAuthor);
    }

    public void delete(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new AuthorNotFoundException(id);
        }
        authorRepository.deleteById(id);
        cacheManager.clearAuthorCache(id);
    }

    @Transactional
    public AuthorDto update(Long id, AuthorDto dto) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Author name cannot be null or empty");
        }

        AuthorMapper.updateFromDto(author, dto);
        Author savedAuthor = authorRepository.save(author);
        cacheManager.clearAuthorCache(id);
        return AuthorMapper.toDto(savedAuthor);
    }
}*/

package com.example.bookshop2.service;

import com.example.bookshop2.dto.AuthorDto;
import com.example.bookshop2.exception.AuthorNotFoundException;
import com.example.bookshop2.exception.ValidationException;
import com.example.bookshop2.mapper.AuthorMapper;
import com.example.bookshop2.model.Author;
import com.example.bookshop2.repository.AuthorRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final CacheManager cacheManager;

    public AuthorService(AuthorRepository authorRepository, CacheManager cacheManager) {
        this.authorRepository = authorRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public List<AuthorDto> findAll() {
        return authorRepository.findAll().stream()
                .map(AuthorMapper::toDto)
                .toList();
    }

    @Transactional
    public AuthorDto findById(Long id) {
        String cacheKey = "author_" + id;
        AuthorDto cachedAuthor = cacheManager.getFromCache(cacheKey, AuthorDto.class);
        if (cachedAuthor != null) {
            return cachedAuthor;
        }

        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
        AuthorDto authorDto = AuthorMapper.toDto(author);
        cacheManager.saveToCache(cacheKey, authorDto);
        return authorDto;
    }

    @Transactional
    public List<AuthorDto> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Search name cannot be null or empty");
        }
        String cacheKey = "author_search_" + name.toLowerCase();
        @SuppressWarnings("unchecked")
        List<AuthorDto> cachedAuthors = cacheManager.getFromCache(cacheKey, List.class);
        if (cachedAuthors != null) {
            return cachedAuthors;
        }

        List<AuthorDto> authors = authorRepository.findByNameContainingIgnoreCase(name).stream()
                .map(AuthorMapper::toDto)
                .toList();
        cacheManager.saveToCache(cacheKey, authors);
        return authors;
    }

    @Transactional
    public AuthorDto create(AuthorDto dto) {
        // Валидация через @NotBlank в DTO, здесь не нужна
        Author author = AuthorMapper.fromDto(dto);
        Author savedAuthor = authorRepository.save(author);
        cacheManager.clearAuthorCache(savedAuthor.getId());
        return AuthorMapper.toDto(savedAuthor);
    }

    @Transactional
    public void delete(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new AuthorNotFoundException(id);
        }
        authorRepository.deleteById(id);
        cacheManager.clearAuthorCache(id);
    }

    @Transactional
    public AuthorDto update(Long id, AuthorDto dto) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));

        AuthorMapper.updateFromDto(author, dto);
        Author savedAuthor = authorRepository.save(author);
        cacheManager.clearAuthorCache(id);
        return AuthorMapper.toDto(savedAuthor);
    }
}