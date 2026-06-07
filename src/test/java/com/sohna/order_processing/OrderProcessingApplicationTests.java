package com.sohna.order_processing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test that verifies the full Spring application
 * context loads correctly with all beans wired together.
 *
 * Uses in-memory H2 database so the file based
 * production database is never touched during tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.sql.init.mode=never",
		"spring.task.scheduling.enabled=false"
})
class OrderProcessingApplicationTests {

	@Test
	void contextLoads() {
		// Verifies the entire Spring context starts without errors.
		// If any bean is misconfigured this test will fail.
	}
}