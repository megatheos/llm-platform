-- LLM Language Learning Platform Database Schema
-- Create database if not exists
CREATE DATABASE IF NOT EXISTS llm_learning DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE llm_learning;

-- User table
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `username` VARCHAR(50) NOT NULL COMMENT 'Username',
    `password` VARCHAR(255) NOT NULL COMMENT 'Encrypted password',
    `email` VARCHAR(100) NOT NULL COMMENT 'Email address',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'Avatar URL',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

-- Word table
CREATE TABLE IF NOT EXISTS `word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `word` VARCHAR(100) NOT NULL COMMENT 'Word text',
    `source_lang` VARCHAR(20) NOT NULL COMMENT 'Source language code',
    `target_lang` VARCHAR(20) NOT NULL COMMENT 'Target language code',
    `definition` TEXT COMMENT 'Word definition',
    `translation` TEXT COMMENT 'Word translation',
    `examples` TEXT COMMENT 'Example sentences (JSON array)',
    `pronunciation` VARCHAR(255) COMMENT 'Pronunciation guide',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word_langs` (`word`, `source_lang`, `target_lang`),
    INDEX `idx_word` (`word`),
    INDEX `idx_source_lang` (`source_lang`),
    INDEX `idx_target_lang` (`target_lang`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Word dictionary table';


-- Word query history table
CREATE TABLE IF NOT EXISTS `word_query_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `word_id` BIGINT NOT NULL COMMENT 'Word ID',
    `query_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Query time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_word_id` (`word_id`),
    INDEX `idx_query_time` (`query_time`),
    CONSTRAINT `fk_wqh_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_wqh_word` FOREIGN KEY (`word_id`) REFERENCES `word` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Word query history table';

-- Scenario table
CREATE TABLE IF NOT EXISTS `scenario` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `name` VARCHAR(100) NOT NULL COMMENT 'Scenario name',
    `description` TEXT COMMENT 'Scenario description',
    `category` VARCHAR(50) NOT NULL COMMENT 'Scenario category',
    `is_preset` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Is preset scenario (1=yes, 0=no)',
    `created_by` BIGINT DEFAULT NULL COMMENT 'Creator user ID (null for preset)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_is_preset` (`is_preset`),
    INDEX `idx_created_by` (`created_by`),
    CONSTRAINT `fk_scenario_user` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dialogue scenario table';

-- Dialogue session table
CREATE TABLE IF NOT EXISTS `dialogue_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `scenario_id` BIGINT NOT NULL COMMENT 'Scenario ID',
    `target_lang` VARCHAR(20) DEFAULT 'en' COMMENT 'Target language for the dialogue',
    `messages` JSON COMMENT 'Conversation messages (JSON array)',
    `started_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Session start time',
    `ended_at` DATETIME DEFAULT NULL COMMENT 'Session end time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_scenario_id` (`scenario_id`),
    INDEX `idx_started_at` (`started_at`),
    CONSTRAINT `fk_ds_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ds_scenario` FOREIGN KEY (`scenario_id`) REFERENCES `scenario` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dialogue session table';

-- Quiz table
CREATE TABLE IF NOT EXISTS `quiz` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `difficulty` VARCHAR(20) NOT NULL COMMENT 'Difficulty level (easy, medium, hard)',
    `target_lang` VARCHAR(20) DEFAULT 'en' COMMENT 'Target language for the quiz',
    `questions` JSON NOT NULL COMMENT 'Quiz questions (JSON array)',
    `total_score` INT NOT NULL DEFAULT 0 COMMENT 'Total possible score',
    `user_score` INT DEFAULT NULL COMMENT 'User achieved score',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Quiz creation time',
    `completed_at` DATETIME DEFAULT NULL COMMENT 'Quiz completion time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_difficulty` (`difficulty`),
    INDEX `idx_created_at` (`created_at`),
    CONSTRAINT `fk_quiz_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quiz table';

-- Learning record table
CREATE TABLE IF NOT EXISTS `learning_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `activity_type` VARCHAR(30) NOT NULL COMMENT 'Activity type (WORD_QUERY, DIALOGUE, QUIZ)',
    `activity_id` BIGINT NOT NULL COMMENT 'Related activity ID',
    `activity_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Activity time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_activity_type` (`activity_type`),
    INDEX `idx_activity_time` (`activity_time`),
    INDEX `idx_user_activity` (`user_id`, `activity_type`, `activity_time`),
    CONSTRAINT `fk_lr_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Learning record table';

-- Insert preset scenarios
INSERT INTO `scenario` (`name`, `description`, `category`, `is_preset`, `created_by`) VALUES
('Airport Check-in', 'Practice conversations at airport check-in counters', 'travel', 1, NULL),
('Hotel Reservation', 'Practice booking and checking into hotels', 'travel', 1, NULL),
('Restaurant Ordering', 'Practice ordering food at restaurants', 'travel', 1, NULL),
('Business Meeting', 'Practice formal business meeting conversations', 'business', 1, NULL),
('Job Interview', 'Practice job interview scenarios', 'business', 1, NULL),
('Email Writing', 'Practice writing professional emails', 'business', 1, NULL),
('Shopping', 'Practice shopping conversations', 'daily_life', 1, NULL),
('Doctor Visit', 'Practice medical appointment conversations', 'daily_life', 1, NULL),
('Making Friends', 'Practice casual conversations and introductions', 'daily_life', 1, NULL),
('Academic Discussion', 'Practice academic discussions and debates', 'academic', 1, NULL),
('Presentation', 'Practice giving academic presentations', 'academic', 1, NULL),
('Research Collaboration', 'Practice research collaboration discussions', 'academic', 1, NULL);
