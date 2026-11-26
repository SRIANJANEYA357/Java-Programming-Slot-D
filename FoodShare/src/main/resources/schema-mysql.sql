-- ========================================================
-- FoodShare Database Schema (MySQL 8.0+)
-- Food Wastage Reduction & Redistribution Platform
-- ========================================================

CREATE DATABASE IF NOT EXISTS foodshare_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE foodshare_db;

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role VARCHAR(30) NOT NULL,
    provider_type VARCHAR(50) DEFAULT 'NONE',
    organization_name VARCHAR(150),
    address VARCHAR(255),
    city VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
) ENGINE=InnoDB;

-- 2. FOOD LISTINGS TABLE
CREATE TABLE IF NOT EXISTS food_listings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    original_quantity INT NOT NULL,
    remaining_quantity INT NOT NULL,
    unit VARCHAR(30) NOT NULL,
    food_type VARCHAR(30) NOT NULL,
    original_price DECIMAL(10,2) DEFAULT 0.00,
    discounted_price DECIMAL(10,2) DEFAULT 0.00,
    location_address VARCHAR(255) NOT NULL,
    location_city VARCHAR(100) NOT NULL,
    expiry_time DATETIME NOT NULL,
    image_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_provider FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_listing_status (status),
    INDEX idx_listing_category (category),
    INDEX idx_listing_city (location_city),
    INDEX idx_listing_expiry (expiry_time)
) ENGINE=InnoDB;

-- 3. FOOD REQUESTS TABLE
CREATE TABLE IF NOT EXISTS food_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    quantity_requested INT NOT NULL,
    total_price DECIMAL(10,2) DEFAULT 0.00,
    is_priority BOOLEAN DEFAULT FALSE,
    priority_category VARCHAR(30) DEFAULT 'REGULAR',
    request_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_listing FOREIGN KEY (listing_id) REFERENCES food_listings(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_request_status (request_status),
    INDEX idx_request_recipient (recipient_id)
) ENGINE=InnoDB;

-- 4. COLLECTION TOKENS TABLE
CREATE TABLE IF NOT EXISTS collection_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL UNIQUE,
    token_code VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at DATETIME NULL,
    verified_by_id BIGINT NULL,
    CONSTRAINT fk_token_request FOREIGN KEY (request_id) REFERENCES food_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_token_verifier FOREIGN KEY (verified_by_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_token_code (token_code),
    INDEX idx_token_status (status)
) ENGINE=InnoDB;

-- 5. VOLUNTEERS TABLE
CREATE TABLE IF NOT EXISTS volunteers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    vehicle_type VARCHAR(50) DEFAULT 'TWO_WHEELER',
    availability_status VARCHAR(30) DEFAULT 'AVAILABLE',
    max_capacity_kg INT DEFAULT 25,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_volunteer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_volunteer_status (availability_status)
) ENGINE=InnoDB;

-- 6. DISTRIBUTION RECORDS TABLE
CREATE TABLE IF NOT EXISTS distribution_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    request_id BIGINT NULL,
    token_id BIGINT NULL,
    provider_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    volunteer_id BIGINT NULL,
    quantity_distributed INT NOT NULL,
    distribution_type VARCHAR(30) NOT NULL,
    camp_name VARCHAR(150) NULL,
    camp_location VARCHAR(255) NULL,
    collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    CONSTRAINT fk_dist_listing FOREIGN KEY (listing_id) REFERENCES food_listings(id),
    CONSTRAINT fk_dist_request FOREIGN KEY (request_id) REFERENCES food_requests(id),
    CONSTRAINT fk_dist_token FOREIGN KEY (token_id) REFERENCES collection_tokens(id),
    CONSTRAINT fk_dist_provider FOREIGN KEY (provider_id) REFERENCES users(id),
    CONSTRAINT fk_dist_recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
    CONSTRAINT fk_dist_volunteer FOREIGN KEY (volunteer_id) REFERENCES users(id),
    INDEX idx_dist_type (distribution_type),
    INDEX idx_dist_collected (collected_at)
) ENGINE=InnoDB;
