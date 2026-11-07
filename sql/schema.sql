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

CREATE TABLE Payment (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID',
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    refunded_at TIMESTAMP NULL DEFAULT NULL,

    CONSTRAINT fk_payment_reservation
        FOREIGN KEY (reservation_id) REFERENCES Reservation(reservation_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_payment_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_payment_refund
        CHECK (refund_amount >= 0 AND refund_amount <= amount),

    CONSTRAINT chk_payment_status
        CHECK (payment_status IN (
            'PAID', 'PARTIALLY_REFUNDED', 'REFUNDED', 'VOID'
        ))
);

CREATE INDEX idx_reservation_room_dates
    ON Reservation (room_id, check_in, check_out);

CREATE INDEX idx_reservation_status_checkin
    ON Reservation (status, check_in);
    
    
CREATE VIEW Available_Rooms_View AS
SELECT
    rm.room_id,
    rm.room_number,
    rm.type_id,
    rt.name AS room_type_name,
    rm.floor,
    rt.capacity,
    rt.price_per_night
FROM Room rm
JOIN RoomType rt ON rm.type_id = rt.type_id;    


CREATE VIEW Active_Guest_Summary_View AS
SELECT
    u.user_id,
    u.name,
    u.email,
    u.phone,
    COUNT(r.reservation_id) AS active_reservation_count
FROM `User` u
LEFT JOIN Reservation r
    ON r.guest_id = u.user_id
    AND r.status IN ('PENDING', 'APPROVED', 'CONFIRMED')
WHERE u.role = 'GUEST'
GROUP BY u.user_id, u.name, u.email, u.phone;