package com.llmplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * View object for Scenario
 */
@Data
public class ScenarioVO {

    private Long id;

    private String name;

    private String description;

    private String category;

    private Boolean isPreset;

    private Long createdBy;

    private LocalDateTime createdAt;
}
