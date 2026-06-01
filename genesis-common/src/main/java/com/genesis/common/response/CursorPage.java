package com.genesis.common.response;

import java.util.List;

/**
 * Cursor (keyset) pagination payload, carried as the {@code data} of an
 * {@link ApiResponse}.
 *
 * <p>
 * Keyset pagination is used for high-volume resources (mentions, clusters)
 * because consumers that drain every page would otherwise pay the O(N&sup2;)
 * cost of deep {@code OFFSET} scans. Each page is fetched by passing the
 * previous page's {@link #nextCursor} back as the {@code cursor} request
 * parameter; the first page omits it. When {@link #hasMore} is {@code false}
 * the traversal is complete and {@link #nextCursor} is {@code null}.
 *
 * @param <T> the type of items in the page
 */
public record CursorPage<T>(
        List<T> items,
        String nextCursor,
        int pageSize,
        boolean hasMore) {

    /** Page size used when the caller supplies a non-positive limit. */
    public static final int DEFAULT_LIMIT = 100;

    /** Hard upper bound on a single page to protect the server heap. */
    public static final int MAX_LIMIT = 500;

    /**
     * Clamp a caller-supplied limit into {@code [1, MAX_LIMIT]}, falling back to
     * {@link #DEFAULT_LIMIT} for non-positive input.
     *
     * @param limit the requested page size
     * @return a safe page size
     */
    public static int clampLimit(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * Factory mirroring the record components for readability at call sites.
     *
     * @param items      the items on this page
     * @param nextCursor the cursor to fetch the next page, or {@code null}
     * @param pageSize   the effective page size
     * @param hasMore    whether more pages remain
     * @param <T>        the item type
     * @return a CursorPage
     */
    public static <T> CursorPage<T> of(List<T> items, String nextCursor, int pageSize, boolean hasMore) {
        return new CursorPage<>(items, nextCursor, pageSize, hasMore);
    }
}
