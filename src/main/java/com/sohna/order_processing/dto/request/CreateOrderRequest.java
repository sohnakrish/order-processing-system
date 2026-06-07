package com.sohna.order_processing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * Request payload for placing a new customer order.
 * Validated before reaching the service layer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreateOrderRequest {

    @NotBlank(message = "Customer name is required.")
    @Size(min = 3, max = 100, message = "Customer name must be between 3 and 100 characters.")
    @Pattern(
            regexp = "^[a-zA-Z\\s'-]+$",
            message = "Customer name must contain letters only."
    )
    private String customerName;

    @NotBlank(message = "Customer email is required.")
    @Size(max = 100, message = "Customer email cannot exceed 100 characters.")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]{3,}@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Please enter a valid email address with at least 3 characters before @."
    )
    private String customerEmail;

    // Optional — if provided must be a valid UUID format
    // Lombok getter suppressed — custom getter below handles blank string conversion
    @Getter(AccessLevel.NONE)
    @Pattern(
            regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$",
            message = "Idempotency key must be a valid UUID format (e.g. 550e8400-e29b-41d4-a716-446655440000)"
    )
    private String idempotencyKey;

    // Without @Valid, item-level constraints are silently skipped
    @NotNull(message = "Items list is required.")
    @NotEmpty(message = "Order must contain at least one item.")
    @Valid
    private List<OrderItemRequest> items;

    /**
     * Returns null if the idempotency key is blank.
     * Prevents empty string from causing unique constraint conflicts
     * when multiple orders are placed without a key.
     */
    public String getIdempotencyKey() {
        return (idempotencyKey != null && idempotencyKey.isBlank())
                ? null
                : idempotencyKey;
    }
}