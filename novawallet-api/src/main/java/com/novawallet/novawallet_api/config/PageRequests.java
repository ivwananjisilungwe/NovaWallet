package com.novawallet.novawallet_api.config;

import com.novawallet.novawallet_api.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Single choke point for client-supplied paging parameters (Phase 1C Batch 2, SEC-10).
 *
 * <p>Rejects negative pages and page sizes outside {@code 1..MAX_PAGE_SIZE} with
 * 400 instead of silently clamping, so a client bug is visible and an
 * attacker cannot request an unbounded page.</p>
 */
public final class PageRequests {

    public static final int MAX_PAGE_SIZE = 100;

    private PageRequests() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        if (page < 0) {
            throw new BadRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }

    public static Pageable of(int page, int size) {
        return of(page, size, Sort.unsorted());
    }
}
