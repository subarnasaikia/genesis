package com.genesis.common.response;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Response wrapper for paginated data.
 *
 * <p>
 * Use this for endpoints that return lists of items with pagination support.
 *
 * @param <T> the type of items in the response
 */
public record PagedResponse<T>(
        boolean success,
        String message,
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        Instant timestamp) {

    /**
     * Creates a PagedResponse from a Spring Data Page object.
     *
     * @param page the Spring Data Page
     * @param <T>  the type of items
     * @return a PagedResponse
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                true,
                "Success",
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                Instant.now());
    }

    /**
     * Creates a PagedResponse from a Spring Data Page object with custom message.
     *
     * @param page    the Spring Data Page
     * @param message custom message
     * @param <T>     the type of items
     * @return a PagedResponse
     */
    public static <T> PagedResponse<T> from(Page<T> page, String message) {
        return new PagedResponse<>(
                true,
                message,
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                Instant.now());
    }
}
