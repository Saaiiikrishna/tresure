package com.treasurehunt.config;

import com.treasurehunt.entity.TreasureHuntPlan;
import com.treasurehunt.service.TreasureHuntPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data initializer to create sample treasure hunt plans
 * DISABLED FOR PRODUCTION - Only runs in development/test profiles
 */
@Component
@Profile({"development", "test", "!production"})
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
                new BigDecimal("2499.17")
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
                new BigDecimal("2074.17")
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
                new BigDecimal("3817.17")
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
                new BigDecimal("4398.17")
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
                new BigDecimal("7469.17")
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
                new BigDecimal("6307.17")
        );
        // Set as team-based plan with 4 members
        advancedPlan2.setTeamType(TreasureHuntPlan.TeamType.TEAM);
        advancedPlan2.setTeamSize(4);

        // Set prize money for all plans
        beginnerPlan.setPrizeMoney(new BigDecimal("5000.00"));      // Urban Explorer
        beginnerPlan2.setPrizeMoney(new BigDecimal("3000.00"));     // Park Adventure
        intermediatePlan.setPrizeMoney(new BigDecimal("8000.00"));  // Mystery of the Lost Museum
        intermediatePlan2.setPrizeMoney(new BigDecimal("10000.00")); // Downtown Detective
        advancedPlan.setPrizeMoney(new BigDecimal("25000.00"));     // The Ultimate Challenge
        advancedPlan2.setPrizeMoney(new BigDecimal("15000.00"));    // Nighttime Expedition

        // Set date/time fields for all plans (required for new validation)
        LocalDate eventDate = LocalDate.now().plusDays(7); // Next week
        LocalTime startTime = LocalTime.of(9, 0); // 9:00 AM

        // Set event dates and times for all plans
        beginnerPlan.setEventDate(eventDate);
        beginnerPlan.setStartTime(startTime);
        beginnerPlan.setEndDate(eventDate);
        beginnerPlan.setEndTime(startTime.plusHours(3)); // 3 hour duration

        beginnerPlan2.setEventDate(eventDate.plusDays(1));
        beginnerPlan2.setStartTime(startTime);
        beginnerPlan2.setEndDate(eventDate.plusDays(1));
        beginnerPlan2.setEndTime(startTime.plusHours(2)); // 2 hour duration

        intermediatePlan.setEventDate(eventDate.plusDays(2));
        intermediatePlan.setStartTime(startTime);
        intermediatePlan.setEndDate(eventDate.plusDays(2));
        intermediatePlan.setEndTime(startTime.plusHours(4)); // 4 hour duration

        intermediatePlan2.setEventDate(eventDate.plusDays(3));
        intermediatePlan2.setStartTime(startTime);
        intermediatePlan2.setEndDate(eventDate.plusDays(3));
        intermediatePlan2.setEndTime(startTime.plusHours(5)); // 5 hour duration

        advancedPlan.setEventDate(eventDate.plusDays(4));
        advancedPlan.setStartTime(startTime);
        advancedPlan.setEndDate(eventDate.plusDays(4));
        advancedPlan.setEndTime(startTime.plusHours(8)); // 8 hour duration

        advancedPlan2.setEventDate(eventDate.plusDays(5));
        advancedPlan2.setStartTime(startTime);
        advancedPlan2.setEndDate(eventDate.plusDays(5));
        advancedPlan2.setEndTime(startTime.plusHours(6)); // 6 hour duration



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
