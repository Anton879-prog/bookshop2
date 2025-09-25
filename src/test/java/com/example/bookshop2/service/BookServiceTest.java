package com.example.bookshop2.service;

import com.example.bookshop2.dto.BookDto;
import com.example.bookshop2.exception.AuthorNotFoundException;
import com.example.bookshop2.exception.BookNotFoundException;
import com.example.bookshop2.exception.PublisherNotFoundException;
import com.example.bookshop2.exception.ValidationException;
import com.example.bookshop2.model.Author;
import com.example.bookshop2.model.Book;
import com.example.bookshop2.model.Publisher;
import com.example.bookshop2.repository.AuthorRepository;
import com.example.bookshop2.repository.BookRepository;
import com.example.bookshop2.repository.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private BookService bookService;

    private Book book;
    private BookDto bookDto;
    private Author author;
    private Publisher publisher;

    // Заглушка для BookMapper
    static class BookMapper {
        private BookMapper() {}

        static BookDto toDto(Book book) {
            BookDto dto = new BookDto();
            dto.setId(book.getId());
            dto.setName(book.getName());
            dto.setPrice(book.getPrice());
            dto.setPublisherId(book.getPublisher().getId());
            dto.setAuthorIds(book.getAuthors().stream().map(Author::getId).collect(Collectors.toSet()));
            return dto;
        }

        static Book fromDto(BookDto dto, Set<Author> authors, Publisher publisher) {
            Book book = new Book();
            book.setName(dto.getName());
            book.setPrice(dto.getPrice());
            book.setPublisher(publisher);
            book.setAuthors(authors);
            return book;
        }

        static void updateFromDto(Book book, BookDto dto, Set<Author> authors, Publisher publisher) {
            if (dto.getName() != null) book.setName(dto.getName());
            if (dto.getPrice() != null) book.setPrice(dto.getPrice());
            if (publisher != null) book.setPublisher(publisher);
            if (authors != null) book.setAuthors(authors);
        }
    }

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setName("John Doe");

        publisher = new Publisher();
        publisher.setId(1L);
        publisher.setName("Acme Publishing");

        book = new Book();
        book.setId(1L);
        book.setName("Test Book");
        book.setPrice(29.99);
        book.setPublisher(publisher);
        book.setAuthors(Set.of(author));

        bookDto = new BookDto();
        bookDto.setId(1L);
        bookDto.setName("Test Book");
        bookDto.setPrice(29.99);
        bookDto.setPublisherId(1L);
        bookDto.setAuthorIds(Set.of(1L));
    }

    @Test
    void findAll_shouldReturnListOfBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(book));

        List<BookDto> result = bookService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Book");
        verify(bookRepository).findAll();
    }

    @Test
    void findById_whenCached_shouldReturnFromCache() {
        when(cacheManager.getFromCache("book_1", BookDto.class)).thenReturn(bookDto);

        BookDto result = bookService.findById(1L);

        assertThat(result.getName()).isEqualTo("Test Book");
        verify(bookRepository, never()).findById(any());
        verify(cacheManager).getFromCache("book_1", BookDto.class);
    }

    @Test
    void findById_whenNotCached_shouldReturnFromDB() {
        when(cacheManager.getFromCache("book_1", BookDto.class)).thenReturn(null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookDto result = bookService.findById(1L);

        assertThat(result.getName()).isEqualTo("Test Book");
        verify(bookRepository).findById(1L);
        verify(cacheManager).saveToCache("book_1", result);
    }

    @Test
    void findById_whenNotFound_shouldThrowBookNotFoundException() {
        when(cacheManager.getFromCache("book_99", BookDto.class)).thenReturn(null);
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.findById(99L));
        verify(bookRepository).findById(99L);
    }

    @Test
    void create_whenValid_shouldCreateBook() {
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        BookDto result = bookService.create(bookDto);

        assertThat(result.getName()).isEqualTo("Test Book");
        verify(bookRepository).save(any(Book.class));
        verify(cacheManager).clearPublisherCache("Acme Publishing");
        verify(cacheManager).clearBookCache(1L);
        verify(cacheManager).clearAuthorCache(1L);
    }

    @Test
    void create_whenPublisherIdNull_shouldThrowValidationException() {
        bookDto.setPublisherId(null);

        assertThrows(ValidationException.class, () -> bookService.create(bookDto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void create_whenAuthorIdsEmpty_shouldThrowValidationException() {
        bookDto.setAuthorIds(Set.of());

        assertThrows(ValidationException.class, () -> bookService.create(bookDto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void create_whenPublisherNotFound_shouldThrowPublisherNotFoundException() {
        when(publisherRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PublisherNotFoundException.class, () -> bookService.create(bookDto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void create_whenAuthorNotFound_shouldThrowAuthorNotFoundException() {
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AuthorNotFoundException.class, () -> bookService.create(bookDto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void delete_whenExists_shouldDelete() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.delete(1L);

        verify(bookRepository).deleteById(1L);
        verify(cacheManager).clearPublisherCache("Acme Publishing");
        verify(cacheManager).clearBookCache(1L);
        verify(cacheManager).clearAuthorCache(1L);
    }

    @Test
    void delete_whenNotExists_shouldThrowBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.delete(99L));
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    void update_whenValid_shouldUpdateBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(book)).thenReturn(book);

        BookDto result = bookService.update(1L, bookDto);

        assertThat(result.getName()).isEqualTo("Test Book");
        verify(bookRepository).save(book);
        verify(cacheManager, times(2)).clearPublisherCache("Acme Publishing"); // Called twice: old and new publisher
        verify(cacheManager).clearBookCache(1L);
        verify(cacheManager, times(2)).clearAuthorCache(1L); // Called twice: old and new authors
    }

    @Test
    void update_whenNotFound_shouldThrowBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.update(99L, bookDto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void update_whenPublisherNotFound_shouldThrowPublisherNotFoundException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(publisherRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PublisherNotFoundException.class, () -> bookService.update(1L, bookDto));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void update_withNullPublisherId_shouldUpdateWithoutPublisherChange() {
        bookDto.setPublisherId(null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(book)).thenReturn(book);

        BookDto result = bookService.update(1L, bookDto);

        assertThat(result.getName()).isEqualTo("Test Book");
        verify(bookRepository).save(book);
        verify(cacheManager).clearPublisherCache("Acme Publishing"); // Only old publisher cleared
        verify(cacheManager).clearBookCache(1L);
        verify(cacheManager, times(2)).clearAuthorCache(1L); // Called twice: old and new authors
    }

    @Test
    void findByPublisherId_whenCached_shouldReturnFromCache() {
        List<BookDto> cachedBooks = List.of(bookDto);
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher)); // Mock publisher check
        when(cacheManager.getFromCache("publishers_1", List.class)).thenReturn(cachedBooks);

        List<BookDto> result = bookService.findByPublisherId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Book");
        verify(bookRepository, never()).findByPublisherId(any());
        verify(publisherRepository).findById(1L);
    }

    @Test
    void findByPublisherId_whenNotCached_shouldReturnFromDB() {
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(cacheManager.getFromCache("publishers_1", List.class)).thenReturn(null);
        when(bookRepository.findByPublisherId(1L)).thenReturn(List.of(book));

        List<BookDto> result = bookService.findByPublisherId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Book");
        verify(bookRepository).findByPublisherId(1L);
        verify(cacheManager).saveToCache("publishers_1", result);
    }

    @Test
    void findByPublisherId_whenPublisherNotFound_shouldThrowPublisherNotFoundException() {
        when(publisherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PublisherNotFoundException.class, () -> bookService.findByPublisherId(99L));
        verify(bookRepository, never()).findByPublisherId(any());
    }

//    @Test
//    void findByPublisherName_whenCached_shouldReturnFromCache() {
//        List<BookDto> cachedBooks = List.of(bookDto);
//        when(publisherRepository.findByName("Acme Publishing")).thenReturn(Optional.of(publisher)); // Mock publisher check
//        when(cacheManager.getFromCache("publishers_name_Acme Publishing", List.class)).thenReturn(cachedBooks);
//
//        List<BookDto> result = bookService.findByPublisherName("Acme Publishing");
//
//        assertThat(result).hasSize(1);
//        assertThat(result.get(0).getName()).isEqualTo("Test Book");
//        verify(bookRepository, never()).findByPublisherNameNative(any());
//        verify(publisherRepository).findByName("Acme Publishing");
//    }

    @Test
    void findByPublisherName_whenPublisherExists_shouldReturnFromDB() {
        when(publisherRepository.findByName("Acme Publishing"))
                .thenReturn(Optional.of(publisher));
        when(bookRepository.findByPublisherNameNative("Acme Publishing"))
                .thenReturn(List.of(book));

        List<BookDto> result = bookService.findByPublisherName("Acme Publishing");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Book");
        verify(bookRepository).findByPublisherNameNative("Acme Publishing");
        verify(cacheManager, never()).getFromCache(any(), any());
        verify(cacheManager, never()).saveToCache(any(), any());
    }

    @Test
    void findByPublisherName_whenPublisherNotFound_shouldThrowPublisherNotFoundException() {
        when(publisherRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(PublisherNotFoundException.class, () -> bookService.findByPublisherName("Unknown"));
        verify(bookRepository, never()).findByPublisherNameNative(any());
        verify(cacheManager, never()).getFromCache(any(), any());
        verify(cacheManager, never()).saveToCache(any(), any());
    }

    @Test
    void findByPriceRange_whenValid_shouldReturnBooks() {
        when(cacheManager.getFromCache("books_price_10.0_50.0", List.class)).thenReturn(null);
        when(bookRepository.findByPriceBetween(10.0, 50.0)).thenReturn(List.of(book));

        List<BookDto> result = bookService.findByPriceRange(10.0, 50.0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Book");
        verify(bookRepository).findByPriceBetween(10.0, 50.0);
        verify(cacheManager).saveToCache("books_price_10.0_50.0", result);
    }

    @Test
    void findByPriceRange_whenCached_shouldReturnFromCache() {
        List<BookDto> cachedBooks = List.of(bookDto);
        when(cacheManager.getFromCache("books_price_10.0_50.0", List.class)).thenReturn(cachedBooks);

        List<BookDto> result = bookService.findByPriceRange(10.0, 50.0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Book");
        verify(bookRepository, never()).findByPriceBetween(anyDouble(), anyDouble());
    }

    @Test
    void findByPriceRange_whenNullPrice_shouldThrowValidationException() {
        assertThrows(ValidationException.class, () -> bookService.findByPriceRange(null, 50.0));
        assertThrows(ValidationException.class, () -> bookService.findByPriceRange(10.0, null));
        verify(bookRepository, never()).findByPriceBetween(anyDouble(), anyDouble());
    }

    @Test
    void findByPriceRange_whenNegativePrice_shouldThrowValidationException() {
        assertThrows(ValidationException.class, () -> bookService.findByPriceRange(-10.0, 50.0));
        assertThrows(ValidationException.class, () -> bookService.findByPriceRange(10.0, -50.0));
        verify(bookRepository, never()).findByPriceBetween(anyDouble(), anyDouble());
    }

    @Test
    void findByPriceRange_whenMinGreaterThanMax_shouldThrowValidationException() {
        assertThrows(ValidationException.class, () -> bookService.findByPriceRange(50.0, 10.0));
        verify(bookRepository, never()).findByPriceBetween(anyDouble(), anyDouble());
    }

    @Test
    void addBooksBulk_whenValid_shouldAddBooks() {
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.saveAll(anyList())).thenReturn(List.of(book));

        List<BookDto> result = bookService.addBooksBulk(List.of(bookDto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Book");
        verify(bookRepository).saveAll(anyList());
        verify(cacheManager).clearPublisherCache("Acme Publishing");
        verify(cacheManager).clearBookCache(1L);
        verify(cacheManager).clearAuthorCache(1L);
    }

    @Test
    void addBooksBulk_whenEmptyList_shouldThrowValidationException() {
        assertThrows(ValidationException.class, () -> bookService.addBooksBulk(List.of()));
        verify(bookRepository, never()).saveAll(any());
    }

    @Test
    void addBooksBulk_whenInvalidBook_shouldThrowValidationException() {
        BookDto invalidDto = new BookDto();
        invalidDto.setName("Invalid Book");
        invalidDto.setPublisherId(null); // Invalid publisherId

        assertThrows(ValidationException.class, () -> bookService.addBooksBulk(List.of(invalidDto)));
        verify(bookRepository, never()).saveAll(any());
    }
}