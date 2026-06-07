package com.sohna.order_processing.dto.response;

import lombok.*;

import java.util.List;

/**
 * Clean pagination wrapper for list order responses.
 *
 * Replaces Spring's verbose Page object with only the fields
 * the client actually needs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedOrderResponse {

    private List<OrderResponse> orders;
    private PaginationMeta pagination;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaginationMeta {
        private int currentPage;
        private int pageSize;
        private long totalOrders;
        private int totalPages;
        private boolean isFirstPage;
        private boolean isLastPage;
    }
}