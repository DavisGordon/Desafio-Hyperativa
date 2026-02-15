CREATE TABLE cards (
    id BINARY(16) NOT NULL,
    encrypted_number VARCHAR(255) NOT NULL,
    number_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uc_cards_number_hash UNIQUE (number_hash)
) ENGINE=InnoDB;
