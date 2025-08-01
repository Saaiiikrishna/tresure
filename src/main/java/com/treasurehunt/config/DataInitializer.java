package com.treasurehunt.config;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.service.TreasureHuntPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Data initializer to create sample treasure hunt plans
 * Runs on application startup to populate the database with initial data
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final TreasureHuntPlanService planService;

    @Autowired
    public DataInitializer(TreasureHuntPlanService planService) {
        this.planService = planService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing sample data...");
        
        try {
            // Check if data already exists
            if (planService.getActivePlanCount() > 0) {
                logger.info("Sample data already exists, skipping initialization");
                return;
            }

            createSamplePlans();
            logger.info("Sample data initialization completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during data initialization", e);
        }
    }

    /**
     * Create sample treasure hunt plans
     */
    private void createSamplePlans() {
        // Beginner Plan - Individual
        TreasureHuntPlan beginnerPlan = new TreasureHuntPlan(
                "Urban Explorer",
                "Perfect for first-time treasure hunters! Explore the city center with easy-to-solve puzzles and clues. " +
                "This adventure takes you through historic landmarks and popular attractions while learning about local history. " +
                "Great for families and groups looking for a fun, educational experience. All clues are straightforward and " +
                "designed to build confidence in treasure hunting skills.",
                3,
                TreasureHuntPlan.DifficultyLevel.BEGINNER,
                15,
                new BigDecimal("29.99")
        );
        // Individual plan (default)

        TreasureHuntPlan beginnerPlan2 = new TreasureHuntPlan(
                "Park Adventure",
                "A delightful outdoor treasure hunt in the beautiful Central Park. Enjoy nature while solving simple riddles " +
                "and finding hidden treasures. This adventure combines physical activity with mental challenges, perfect for " +
                "team building or family outings. Discover secret spots in the park that most visitors never see!",
                2,
                TreasureHuntPlan.DifficultyLevel.BEGINNER,
                20,
                new BigDecimal("24.99")
        );
        // Set as team-based plan
        beginnerPlan2.setTeamType(TreasureHuntPlan.TeamType.TEAM);
        beginnerPlan2.setTeamSize(2);

        // Intermediate Plans
        TreasureHuntPlan intermediatePlan = new TreasureHuntPlan(
                "Mystery of the Lost Museum",
                "Dive deeper into the world of treasure hunting with this museum-based adventure. Solve complex puzzles " +
                "related to art, history, and science while navigating through multiple floors of exhibits. This hunt " +
                "requires critical thinking and attention to detail. You'll need to decode ancient symbols, solve mathematical " +
                "puzzles, and piece together historical clues to find the ultimate treasure.",
                4,
                TreasureHuntPlan.DifficultyLevel.INTERMEDIATE,
                12,
                new BigDecimal("45.99")
        );
        // Set as team-based plan with 3 members
        intermediatePlan.setTeamType(TreasureHuntPlan.TeamType.TEAM);
        intermediatePlan.setTeamSize(3);

        TreasureHuntPlan intermediatePlan2 = new TreasureHuntPlan(
                "Downtown Detective",
                "Put on your detective hat and solve a thrilling mystery in the heart of downtown. This adventure combines " +
                "treasure hunting with crime-solving elements. Follow clues through busy streets, interview 'witnesses' " +
                "(actors), and use deductive reasoning to crack the case. Features moderate physical challenges and " +
                "requires good problem-solving skills.",
                5,
                TreasureHuntPlan.DifficultyLevel.INTERMEDIATE,
                10,
                new BigDecimal("52.99")
        );
        // Individual plan (default)

        // Advanced Plans
        TreasureHuntPlan advancedPlan = new TreasureHuntPlan(
                "The Ultimate Challenge",
                "Our most challenging treasure hunt designed for experienced adventurers! This epic quest spans multiple " +
                "locations across the city and requires advanced problem-solving skills, physical endurance, and teamwork. " +
                "Expect cryptographic puzzles, physical challenges, time pressure, and multi-layered clues that will test " +
                "every aspect of your treasure hunting abilities. Only the most dedicated teams will reach the final treasure!",
                8,
                TreasureHuntPlan.DifficultyLevel.ADVANCED,
                6,
                new BigDecimal("89.99")
        );
        // Set as team-based plan with 5 members
        advancedPlan.setTeamType(TreasureHuntPlan.TeamType.TEAM);
        advancedPlan.setTeamSize(5);

        TreasureHuntPlan advancedPlan2 = new TreasureHuntPlan(
                "Nighttime Expedition",
                "Experience the thrill of treasure hunting after dark! This advanced nighttime adventure uses special " +
                "equipment including UV lights, night vision tools, and GPS devices. Navigate through challenging terrain " +
                "in low-light conditions while solving complex puzzles. This hunt includes elements of orienteering, " +
                "advanced cryptography, and requires excellent teamwork and communication skills.",
                6,
                TreasureHuntPlan.DifficultyLevel.ADVANCED,
                8,
                new BigDecimal("75.99")
        );
        // Set as team-based plan with 4 members
        advancedPlan2.setTeamType(TreasureHuntPlan.TeamType.TEAM);
        advancedPlan2.setTeamSize(4);

        // Save all plans
        try {
            planService.createPlan(beginnerPlan);
            planService.createPlan(beginnerPlan2);
            planService.createPlan(intermediatePlan);
            planService.createPlan(intermediatePlan2);
            planService.createPlan(advancedPlan);
            planService.createPlan(advancedPlan2);
            
            logger.info("Created {} sample treasure hunt plans", 6);
            
        } catch (Exception e) {
            logger.error("Error creating sample plans", e);
        }
    }
}
