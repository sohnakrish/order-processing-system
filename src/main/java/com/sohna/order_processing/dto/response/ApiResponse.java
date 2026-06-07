package com.sohna.order_processing.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Standard wrapper for all API responses.
 *
 * Every endpoint returns this shape so the client always knows
 * what to expect — success or failure, the structure is the same.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    // Automatically set to the time this response was created
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Creates a success response with data and message.
     *
     * @param data    the response payload
     * @param message description of the result
     * @return a successful ApiResponse wrapping the data
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates an error response with no data.
     *
     * @param message description of what went wrong
     * @return a failed ApiResponse with null data
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}