package com.sohna.order_processing.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response payload for a single order line item.
 *
 * lineTotal is included so the client does not have
 * to calculate quantity x productPrice themselves.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private UUID itemId;
    private String productName;
    private String productId;
    private Integer quantity;
    private BigDecimal productPrice;
    private BigDecimal lineTotal;
}