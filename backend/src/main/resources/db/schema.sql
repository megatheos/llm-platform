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

-- =====================================================
-- Personalized Learning System Tables
-- =====================================================

-- Memory records table (间隔重复记忆记录表)
CREATE TABLE IF NOT EXISTS `memory_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `word_id` BIGINT NOT NULL COMMENT 'Word ID',
    `mastery_level` INT NOT NULL DEFAULT 0 COMMENT 'Mastery level (0-100)',
    `last_review_time` DATETIME DEFAULT NULL COMMENT 'Last review time',
    `next_review_time` DATETIME DEFAULT NULL COMMENT 'Next review time',
    `review_count` INT NOT NULL DEFAULT 0 COMMENT 'Review count',
    `correct_count` INT NOT NULL DEFAULT 0 COMMENT 'Correct answer count',
    `wrong_count` INT NOT NULL DEFAULT 0 COMMENT 'Wrong answer count',
    `status` VARCHAR(20) NOT NULL DEFAULT 'LEARNING' COMMENT 'Status: LEARNING, MASTERED, FORGOTTEN',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_word` (`user_id`, `word_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_next_review` (`next_review_time`),
    INDEX `idx_status` (`user_id`, `status`),
    CONSTRAINT `fk_memory_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Memory records table for spaced repetition';

-- Learning profiles table (学习档案表)
CREATE TABLE IF NOT EXISTS `learning_profiles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT 'User ID',
    `preferred_learning_times` JSON COMMENT 'Preferred learning times JSON',
    `weak_areas` JSON COMMENT 'Weak areas JSON',
    `average_daily_words` DECIMAL(10,2) DEFAULT NULL COMMENT 'Average daily words learned',
    `average_accuracy` DECIMAL(5,2) DEFAULT NULL COMMENT 'Average accuracy rate',
    `learning_speed_trend` VARCHAR(20) DEFAULT NULL COMMENT 'Learning speed trend: FAST, NORMAL, SLOW',
    `last_analysis_time` DATETIME DEFAULT NULL COMMENT 'Last analysis time',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Learning profiles table';

-- Study plans table (学习计划表)
CREATE TABLE IF NOT EXISTS `study_plans` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `goal_type` VARCHAR(20) NOT NULL COMMENT 'Goal type: EXAM, TRAVEL, BUSINESS, DAILY',
    `target_date` DATE DEFAULT NULL COMMENT 'Target completion date',
    `daily_task_count` INT NOT NULL COMMENT 'Daily task count',
    `current_phase` VARCHAR(20) DEFAULT NULL COMMENT 'Current phase: BEGINNER, INTERMEDIATE, ADVANCED',
    `completion_rate` DECIMAL(5,2) DEFAULT NULL COMMENT 'Completion rate',
    `adjustment_history` JSON COMMENT 'Adjustment history JSON',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_target_date` (`target_date`),
    CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Study plans table';

-- Daily tasks table (每日任务表)
CREATE TABLE IF NOT EXISTS `daily_tasks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `plan_id` BIGINT NOT NULL COMMENT 'Plan ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `task_date` DATE NOT NULL COMMENT 'Task date',
    `task_type` VARCHAR(20) NOT NULL COMMENT 'Task type: VOCABULARY, GRAMMAR, DIALOGUE, REVIEW',
    `content` JSON COMMENT 'Task content JSON',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'Status: PENDING, IN_PROGRESS, COMPLETED',
    `total_items` INT NOT NULL COMMENT 'Total items in task',
    `completed_items` INT NOT NULL DEFAULT 0 COMMENT 'Completed items count',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `completed_time` DATETIME DEFAULT NULL COMMENT 'Completion time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_date` (`user_id`, `task_date`),
    INDEX `idx_plan_id` (`plan_id`),
    INDEX `idx_status` (`status`),
    CONSTRAINT `fk_task_plan` FOREIGN KEY (`plan_id`) REFERENCES `study_plans` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_task_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Daily tasks table';

-- Achievements table (成就表)
CREATE TABLE IF NOT EXISTS `achievements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Achievement code',
    `name` VARCHAR(100) NOT NULL COMMENT 'Achievement name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'Achievement description',
    `icon_url` VARCHAR(255) DEFAULT NULL COMMENT 'Icon URL',
    `category` VARCHAR(20) NOT NULL COMMENT 'Category: STREAK, MILESTONE, MASTERY',
    `required_value` INT NOT NULL COMMENT 'Required value (days, words count, etc)',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Achievements table';

-- User achievements table (用户成就表)
CREATE TABLE IF NOT EXISTS `user_achievements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `achievement_id` BIGINT NOT NULL COMMENT 'Achievement ID',
    `unlocked_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Unlock time',
    `is_notified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether notification was sent',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_achievement` (`user_id`, `achievement_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_achievement_id` (`achievement_id`),
    CONSTRAINT `fk_ua_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ua_achievement` FOREIGN KEY (`achievement_id`) REFERENCES `achievements` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User achievements table';

-- Learning streaks table (学习连续记录表)
CREATE TABLE IF NOT EXISTS `learning_streaks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT 'User ID',
    `current_streak` INT NOT NULL DEFAULT 0 COMMENT 'Current streak days',
    `longest_streak` INT NOT NULL DEFAULT 0 COMMENT 'Longest streak days',
    `last_learning_date` DATE DEFAULT NULL COMMENT 'Last learning date',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    CONSTRAINT `fk_streak_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Learning streaks table';

-- Insert predefined achievements
INSERT INTO `achievements` (`code`, `name`, `description`, `icon_url`, `category`, `required_value`) VALUES
('STREAK_7', '坚持一周', '连续学习7天', 'streak_7.png', 'STREAK', 7),
('STREAK_30', '坚持一月', '连续学习30天', 'streak_30.png', 'STREAK', 30),
('STREAK_100', '百日坚持', '连续学习100天', 'streak_100.png', 'STREAK', 100),
('WORDS_100', '百词斩', '掌握100个词汇', 'words_100.png', 'MILESTONE', 100),
('WORDS_500', '词汇达人', '掌握500个词汇', 'words_500.png', 'MILESTONE', 500),
('WORDS_1000', '词汇大师', '掌握1000个词汇', 'words_1000.png', 'MILESTONE', 1000),
('ACCURACY_90', '精准学习', '正确率达到90%', 'accuracy_90.png', 'MASTERY', 90),
('REVIEW_100', '复习达人', '完成100次复习', 'review_100.png', 'MILESTONE', 100);
