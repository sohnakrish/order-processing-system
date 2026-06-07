package com.sohna.order_processing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI documentation for the Order Processing API.
 * Swagger UI is available at /swagger-ui.html after the app starts.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderProcessingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Processing API")
                        .description(
                                "REST API for managing e-commerce orders " +
                                        "built for PeerIslands. Handles the complete " +
                                        "order lifecycle from placement through " +
                                        "processing, shipping and delivery."
                        )
                        .version("1.0.0"));
    }
}