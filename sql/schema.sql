-- Hotel Reservation System
-- Role 2: Grant Ross

CREATE DATABASE IF NOT EXISTS hotel_reservation;
USE hotel_reservation;

CREATE TABLE `User` (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(10) NOT NULL DEFAULT 'GUEST',
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'GUEST'))
);

CREATE TABLE RoomType (
    type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    capacity INT NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_roomtype_capacity CHECK (capacity >= 1),
    CONSTRAINT chk_roomtype_price CHECK (price_per_night >= 0)
);

CREATE TABLE Room (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    type_id BIGINT NOT NULL,
    floor INT NOT NULL,
    CONSTRAINT fk_room_type
        FOREIGN KEY (type_id) REFERENCES RoomType(type_id)
        ON DELETE RESTRICT
);

CREATE TABLE Reservation (
    reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_price DECIMAL(10,2) NOT NULL,

    CONSTRAINT fk_reservation_guest
        FOREIGN KEY (guest_id) REFERENCES `User`(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_reservation_room
        FOREIGN KEY (room_id) REFERENCES Room(room_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_reservation_dates
        CHECK (check_out > check_in),

    CONSTRAINT chk_reservation_status
        CHECK (status IN ('PENDING', 'APPROVED', 'CONFIRMED', 'CANCELLED')),

    CONSTRAINT chk_reservation_price
        CHECK (total_price >= 0)
);