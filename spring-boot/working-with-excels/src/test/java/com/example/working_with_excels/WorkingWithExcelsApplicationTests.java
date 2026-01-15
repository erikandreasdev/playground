package com.example.working_with_excels;

import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for {@link WorkingWithExcelsApplication}.
 */
@SpringBootTest
class WorkingWithExcelsApplicationTests {

	/**
	 * callback for context loading verification.
	 */
	@Test
	void contextLoads() {
		// Context loads successfully
	}

	/**
	 * Verifies that the main application method runs without throwing exceptions.
	 */
	@Test
	void testMain() {
		assertThatCode(() -> WorkingWithExcelsApplication.main(new String[] { "--server.port=0" }))
				.doesNotThrowAnyException();
	}

}
