-- H2 compatible schema for testing

-- User table
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE (`username`),
    UNIQUE (`email`)
);

-- Word table
CREATE TABLE IF NOT EXISTS `word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `word` VARCHAR(100) NOT NULL,
    `source_lang` VARCHAR(20) NOT NULL,
    `target_lang` VARCHAR(20) NOT NULL,
    `definition` TEXT,
    `translation` TEXT,
    `examples` TEXT,
    `pronunciation` VARCHAR(255),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

-- Word query history table
CREATE TABLE IF NOT EXISTS `word_query_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `word_id` BIGINT NOT NULL,
    `query_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

-- Scenario table
CREATE TABLE IF NOT EXISTS `scenario` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `category` VARCHAR(50) NOT NULL,
    `is_preset` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_by` BIGINT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);


-- Dialogue session table
CREATE TABLE IF NOT EXISTS `dialogue_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `scenario_id` BIGINT NOT NULL,
    `messages` TEXT,
    `started_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ended_at` TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (`id`)
);

-- Quiz table
CREATE TABLE IF NOT EXISTS `quiz` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `difficulty` VARCHAR(20) NOT NULL,
    `questions` TEXT NOT NULL,
    `total_score` INT NOT NULL DEFAULT 0,
    `user_score` INT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `completed_at` TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (`id`)
);

-- Learning record table
CREATE TABLE IF NOT EXISTS `learning_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `activity_type` VARCHAR(30) NOT NULL,
    `activity_id` BIGINT NOT NULL,
    `activity_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
