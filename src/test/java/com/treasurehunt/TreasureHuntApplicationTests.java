package com.treasurehunt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic integration test for the Treasure Hunt Application
 * Verifies that the Spring Boot application context loads successfully
 */
@SpringBootTest
@ActiveProfiles("test")
class TreasureHuntApplicationTests {

    /**
     * Test that the application context loads without errors
     */
    @Test
    void contextLoads() {
        // This test will pass if the application context loads successfully
        // It verifies that all beans are properly configured and dependencies are resolved
    }
}
