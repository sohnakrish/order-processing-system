package com.sohna.order_processing.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents a single product line item within a create order request.
 * BigDecimal is used for price to avoid floating point rounding errors.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderItemRequest {

    @NotBlank(message = "Product ID is required.")
    @Size(min = 3, max = 50, message = "Product ID must be between 3 and 50 characters.")
    @Pattern(
            regexp = "^[A-Z0-9]+(-[A-Z0-9]+)+$",
            message = "Product ID must be in uppercase with hyphens (e.g. APPL-IPH15-001)"
    )
    private String productId;

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters.")
    private String productName;

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    @Max(value = 100, message = "Quantity cannot exceed 100, please place a separate order for additional items.")
    private Integer quantity;

    // Price of one single product at the time of the request.
    // Zero or negative values are rejected — discounts must be applied
    // at the order level, not by setting a zero unit price.
    @NotNull(message = "Product price is required.")
    @DecimalMin(value = "0.01", message = "Product price must be greater than zero.")
    @Digits(
            integer = 7,
            fraction = 2,
            message = "Product price must not exceed 7 digits and 2 decimal places (e.g. 9999999.99)"
    )
    private BigDecimal productPrice;
}