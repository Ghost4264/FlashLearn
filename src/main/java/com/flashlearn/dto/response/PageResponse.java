package com.flashlearn.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Универсальная обёртка для постраничных ответов
 *
 * @param content       элементы текущей страницы
 * @param page          номер текущей страницы
 * @param size          размер страницы
 * @param totalElements общее количество элементов
 * @param totalPages    общее количество страниц
 * @param last          true если это последняя страница
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
