package com.llmplatform.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.llmplatform.entity.Scenario;
import com.llmplatform.mapper.ScenarioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Data initializer for preset scenarios
 * Ensures preset scenarios exist in the database on application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ScenarioMapper scenarioMapper;

    @Override
    public void run(String... args) {
        initializePresetScenarios();
    }

    private void initializePresetScenarios() {
        // Check if preset scenarios already exist
        Long existingCount = scenarioMapper.selectCount(
            new LambdaQueryWrapper<Scenario>().eq(Scenario::getIsPreset, true)
        );

        if (existingCount > 0) {
            log.info("Preset scenarios already exist ({} found), skipping initialization", existingCount);
            return;
        }

        log.info("Initializing preset scenarios...");

        List<Scenario> presetScenarios = Arrays.asList(
            // Travel scenarios
            createPresetScenario("Airport Check-in", 
                "Practice conversations at airport check-in counters", "travel"),
            createPresetScenario("Hotel Reservation", 
                "Practice booking and checking into hotels", "travel"),
            createPresetScenario("Restaurant Ordering", 
                "Practice ordering food at restaurants", "travel"),
            
            // Business scenarios
            createPresetScenario("Business Meeting", 
                "Practice formal business meeting conversations", "business"),
            createPresetScenario("Job Interview", 
                "Practice job interview scenarios", "business"),
            createPresetScenario("Email Writing", 
                "Practice writing professional emails", "business"),

            // Daily life scenarios
            createPresetScenario("Shopping", 
                "Practice shopping conversations", "daily_life"),
            createPresetScenario("Doctor Visit", 
                "Practice medical appointment conversations", "daily_life"),
            createPresetScenario("Making Friends", 
                "Practice casual conversations and introductions", "daily_life"),
            
            // Academic scenarios
            createPresetScenario("Academic Discussion", 
                "Practice academic discussions and debates", "academic"),
            createPresetScenario("Presentation", 
                "Practice giving academic presentations", "academic"),
            createPresetScenario("Research Collaboration", 
                "Practice research collaboration discussions", "academic")
        );

        for (Scenario scenario : presetScenarios) {
            scenarioMapper.insert(scenario);
        }

        log.info("Successfully initialized {} preset scenarios", presetScenarios.size());
    }

    private Scenario createPresetScenario(String name, String description, String category) {
        Scenario scenario = new Scenario();
        scenario.setName(name);
        scenario.setDescription(description);
        scenario.setCategory(category);
        scenario.setIsPreset(true);
        scenario.setCreatedBy(null);
        scenario.setCreatedAt(LocalDateTime.now());
        return scenario;
    }
}
