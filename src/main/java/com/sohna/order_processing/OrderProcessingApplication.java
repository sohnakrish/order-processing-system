package com.sohna.order_processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Order Processing application.
 * EnableScheduling activates the background job that moves
 * PENDING orders to PROCESSING every 5 minutes.
 */

@SpringBootApplication
@EnableScheduling
public class OrderProcessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderProcessingApplication.class, args);
	}

}
