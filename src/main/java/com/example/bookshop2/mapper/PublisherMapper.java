package com.example.bookshop2.mapper;

import com.example.bookshop2.dto.PublisherDto;
import com.example.bookshop2.model.Publisher;

public class PublisherMapper {
    private PublisherMapper() {}

    public static PublisherDto toDto(Publisher publisher) {
        PublisherDto dto = new PublisherDto();
        dto.setId(publisher.getId());
        dto.setName(publisher.getName());
        return dto;
    }

    public static Publisher fromDto(PublisherDto dto) {
        Publisher publisher = new Publisher();
        publisher.setName(dto.getName());
        return publisher;
    }

    public static void updateFromDto(Publisher publisher, PublisherDto dto) {
        if (dto.getName() != null) {
            publisher.setName(dto.getName());
        }
    }
}