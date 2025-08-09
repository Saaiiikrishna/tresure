package com.treasurehunt;

import com.treasurehunt.config.TreasureHuntTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test to verify that the Spring ApplicationContext loads successfully
 * This test ensures that all configuration issues have been resolved
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TreasureHuntTestConfiguration.class)
class ApplicationContextTest {

    /**
     * Test that the Spring ApplicationContext loads without errors
     * If this test passes, it means all beans are properly configured
     * and there are no circular dependencies or missing beans
     */
    @Test
    void contextLoads() {
        // This test will pass if the ApplicationContext loads successfully
        // The @SpringBootTest annotation will attempt to load the full application context
        // If there are any configuration issues, this test will fail
    }
}
