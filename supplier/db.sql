CREATE DATABASE IF NOT EXISTS dapp;

CREATE TABLE authorized_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    associated_entity_name VARCHAR(100),
    auth_level INT,
    token VARCHAR(36),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO authorized_tokens (associated_entity_name, auth_level, token) VALUES ('test token', 1, 'fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d');
