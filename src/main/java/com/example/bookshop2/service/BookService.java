package com.example.bookshop2.service;

import com.example.bookshop2.dto.BookDto;
import com.example.bookshop2.exception.AuthorNotFoundException;
import com.example.bookshop2.exception.BookNotFoundException;
import com.example.bookshop2.exception.PublisherNotFoundException;
import com.example.bookshop2.exception.ValidationException;
import com.example.bookshop2.mapper.BookMapper;
import com.example.bookshop2.model.Author;
import com.example.bookshop2.model.Book;
import com.example.bookshop2.model.Publisher;
import com.example.bookshop2.repository.AuthorRepository;
import com.example.bookshop2.repository.BookRepository;
import com.example.bookshop2.repository.PublisherRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BookService {
    private static final Logger LOG = LoggerFactory.getLogger(BookService.class);
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final CacheManager cacheManager;

    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository,
                       PublisherRepository publisherRepository,
                       CacheManager cacheManager) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public List<BookDto> findAll() {
        return bookRepository.findAll().stream()
                .map(BookMapper::toDto)
                .toList();
    }

    @Transactional
    public BookDto findById(Long id) {
        String cacheKey = "book_" + id;
        BookDto cachedBook = cacheManager.getFromCache(cacheKey, BookDto.class);
        if (cachedBook != null) {
            return cachedBook;
        }

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        BookDto bookDto = BookMapper.toDto(book);
        cacheManager.saveToCache(cacheKey, bookDto);
        return bookDto;
    }

    @Transactional
    public BookDto create(BookDto dto) {
        if (dto.getPublisherId() == null) {
            throw new ValidationException("Publisher ID cannot be null");
        }
        if (dto.getAuthorIds() == null || dto.getAuthorIds().isEmpty()) {
            throw new ValidationException("At least one author ID is required");
        }

        Publisher publisher = publisherRepository.findById(dto.getPublisherId())
                .orElseThrow(() -> new PublisherNotFoundException(dto.getPublisherId()));

        Set<Author> authors = dto.getAuthorIds().stream()
                .map(id -> authorRepository.findById(id)
                        .orElseThrow(() -> new AuthorNotFoundException(id)))
                .collect(Collectors.toSet());

        Book book = BookMapper.fromDto(dto, authors, publisher);
        Book savedBook = bookRepository.save(book);

        cacheManager.clearPublisherCache(publisher.getName());
        cacheManager.clearBookCache(savedBook.getId());
        authors.forEach(author -> cacheManager.clearAuthorCache(author.getId()));

        return BookMapper.toDto(savedBook);
    }

    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        String publisherName = book.getPublisher() != null ? book.getPublisher().getName() : null;

        bookRepository.deleteById(id);

        Set<Long> authorIds = book.getAuthors().stream()
                .map(Author::getId)
                .collect(Collectors.toSet());

        if (publisherName != null) {
            cacheManager.clearPublisherCache(publisherName);
        }
        cacheManager.clearBookCache(id);
        authorIds.forEach(cacheManager::clearAuthorCache);
    }

    @Transactional
    public BookDto update(Long id, BookDto dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        Publisher publisher = null;
        if (dto.getPublisherId() != null) {
            publisher = publisherRepository.findById(dto.getPublisherId())
                    .orElseThrow(() -> new PublisherNotFoundException(dto.getPublisherId()));
        }

        String oldPublisherName = book.getPublisher() != null ? book.getPublisher().getName() : null;

        Set<Author> authors = null;
        if (dto.getAuthorIds() != null) {
            authors = dto.getAuthorIds().stream()
                    .map(authorId -> authorRepository.findById(authorId)
                            .orElseThrow(() -> new AuthorNotFoundException(authorId)))
                    .collect(Collectors.toSet());
        }

        BookMapper.updateFromDto(book, dto, authors, publisher);

        if (oldPublisherName != null) {
            cacheManager.clearPublisherCache(oldPublisherName);
        }
        if (publisher != null) {
            cacheManager.clearPublisherCache(publisher.getName());
        }
        cacheManager.clearBookCache(id);

        Book savedBook = bookRepository.save(book);

        Set<Long> oldAuthorIds = book.getAuthors().stream()
                .map(Author::getId)
                .collect(Collectors.toSet());

        oldAuthorIds.forEach(cacheManager::clearAuthorCache);
        if (authors != null) {
            authors.forEach(author -> cacheManager.clearAuthorCache(author.getId()));
        }

        return BookMapper.toDto(savedBook);
    }

    @Transactional
    public List<BookDto> findByPublisherId(Long publisherId) {
        publisherRepository.findById(publisherId)
                .orElseThrow(() -> new PublisherNotFoundException(publisherId));

        String cacheKey = "publishers_" + publisherId;
        @SuppressWarnings("unchecked")
        List<BookDto> cachedBooks = cacheManager.getFromCache(cacheKey, List.class);
        if (cachedBooks != null) {
            return cachedBooks;
        }

        List<BookDto> books = bookRepository.findByPublisherId(publisherId).stream()
                .map(BookMapper::toDto)
                .toList();

        cacheManager.saveToCache(cacheKey, books);
        return books;
    }

    @Transactional
    public List<BookDto> findByPublisherName(String publisherName) {
        publisherRepository.findByName(publisherName)
                .orElseThrow(() -> new PublisherNotFoundException(publisherName));

        return bookRepository.findByPublisherNameNative(publisherName).stream()
                .map(BookMapper::toDto)
                .toList();
    }

    @Transactional
    public List<BookDto> findByPriceRange(Double minPrice, Double maxPrice) {
        if (minPrice == null || maxPrice == null) {
            throw new ValidationException("Min and max price cannot be null");
        }
        if (minPrice < 0 || maxPrice < 0) {
            throw new ValidationException("Min and max price must be non-negative");
        }
        if (minPrice > maxPrice) {
            throw new ValidationException("Min price cannot be greater than max price");
        }

        String cacheKey = "books_price_" + minPrice + "_" + maxPrice;
        @SuppressWarnings("unchecked")
        List<BookDto> cachedBooks = cacheManager.getFromCache(cacheKey, List.class);
        if (cachedBooks != null) {
            return cachedBooks;
        }

        List<BookDto> books = bookRepository.findByPriceBetween(minPrice, maxPrice).stream()
                .map(BookMapper::toDto)
                .toList();

        cacheManager.saveToCache(cacheKey, books);
        return books;
    }

    @Transactional
    public List<BookDto> addBooksBulk(List<BookDto> books) {
        if (books == null || books.isEmpty()) {
            throw new ValidationException("Book list cannot be empty");
        }

        List<Book> entities = books.stream()
                .map(dto -> {
                    if (dto.getPublisherId() == null) {
                        throw new ValidationException("Publisher ID cannot be null for book: " + dto.getName());
                    }
                    if (dto.getAuthorIds() == null || dto.getAuthorIds().isEmpty()) {
                        throw new ValidationException("At least one author ID is required for book: " + dto.getName());
                    }
                    if (dto.getPrice() == null || dto.getPrice() <= 0) {
                        throw new ValidationException("Price must be positive for book: " + dto.getName());
                    }

                    Publisher publisher = publisherRepository.findById(dto.getPublisherId())
                            .orElseThrow(() -> new PublisherNotFoundException(dto.getPublisherId()));
                    Set<Author> authors = dto.getAuthorIds().stream()
                            .map(id -> authorRepository.findById(id)
                                    .orElseThrow(() -> new AuthorNotFoundException(id)))
                            .collect(Collectors.toSet());

                    return BookMapper.fromDto(dto, authors, publisher);
                })
                .toList();

        List<Book> savedBooks = bookRepository.saveAll(entities);
        savedBooks.forEach(book -> {
            cacheManager.clearPublisherCache(book.getPublisher().getName());
            cacheManager.clearBookCache(book.getId());
            book.getAuthors().forEach(author -> cacheManager.clearAuthorCache(author.getId()));
        });

        LOG.info("Добавлено {} книг", savedBooks.size());
        return savedBooks.stream().map(BookMapper::toDto).toList();
    }
}