CREATE DATABASE IF NOT EXISTS dapp;

CREATE TABLE authorized_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    associated_entity_name VARCHAR(100),
    auth_level INT,
    token VARCHAR(36),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO authorized_tokens (associated_entity_name, auth_level, token) VALUES ('test token', 1, 'fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d');
INSERT INTO authorized_tokens (associated_entity_name, auth_level, token) VALUES ('Admin', 2, 'ga3b2c9c-a96d-48a8-82ad-0cb775dd3e5d');
INSERT INTO authorized_tokens (associated_entity_name, auth_level, token) VALUES ('Other webshop', 3, 'ha3b2c9c-a96d-48a8-82ad-0cb775dd3e5d');

CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    price DECIMAL(9,2),
    quantity INT,
    description TEXT,
    image_url VARCHAR(200),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO products (name, price, quantity, description, image_url) VALUES ('habeja', 420.69, 9, 'Mo how zeh!', 'https://rgbeast.francecentral.cloudapp.azure.com/assets/test.jpg');

CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token_id INT, 
    global_order_id VARCHAR(100) UNIQUE, 
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(token_id) REFERENCES authorized_tokens(id)
);

CREATE TABLE reservations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token_id INT, 
    global_order_id VARCHAR(100) UNIQUE, 
    status INT, 
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(token_id) REFERENCES authorized_tokens(id)
);

CREATE TABLE reservation_tracker (
    id INT AUTO_INCREMENT PRIMARY KEY,
    amount INT,
    product_id INT,
    reservation_id INT,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(product_id) REFERENCES products(id),
    FOREIGN KEY(reservation_id) REFERENCES reservations(id)
);

CREATE TABLE order_tracker (
    id INT AUTO_INCREMENT PRIMARY KEY,
    amount INT,
    product_id INT,
    order_id INT,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(product_id) REFERENCES products(id),
    FOREIGN KEY(order_id) REFERENCES orders(id)
);
