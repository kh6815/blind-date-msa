CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    birth_date DATE NOT NULL,
    mbti VARCHAR(10),
    interests VARCHAR(255),
    profile_image_url VARCHAR(255),
    CONSTRAINT uk_users_email UNIQUE (email)
);
