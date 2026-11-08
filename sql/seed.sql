-- Hotel Reservation System
-- Role 2: Grant Ross
-- Sample data for development and testing

USE hotel_reservation;

-- Sample capacities and prices, chosen for testing.
INSERT INTO RoomType (name, description, capacity, price_per_night)
VALUES
    ('Standard', 'Standard room for two guests', 2, 100.00),
    ('Deluxe', 'Deluxe room for four guests', 4, 150.00),
    ('Suite', 'Suite for six guests', 6, 250.00);
    
    
    -- Sample room inventory.
INSERT INTO Room (room_number, type_id, floor)
SELECT '101', type_id, 1 FROM RoomType WHERE name = 'Standard'
UNION ALL
SELECT '102', type_id, 1 FROM RoomType WHERE name = 'Standard'
UNION ALL
SELECT '201', type_id, 2 FROM RoomType WHERE name = 'Deluxe'
UNION ALL
SELECT '202', type_id, 2 FROM RoomType WHERE name = 'Deluxe'
UNION ALL
SELECT '301', type_id, 3 FROM RoomType WHERE name = 'Suite'
UNION ALL
SELECT '302', type_id, 3 FROM RoomType WHERE name = 'Suite';


-- Demo users. Passwords are stored as PBKDF2 hashes.
INSERT INTO `User` (email, password_hash, name, phone, role)
VALUES
(
    'admin@example.com',
    'pbkdf2-sha256$210000$OKrgWOAfmDKiIJS5AJwI8Q==$PIWrnoZuPPv4baL4U6Z5UHqcM/MjAdyI/s9p2Soycwg=',
    'Demo Admin',
    NULL,
    'ADMIN'
),
(
    'guest@example.com',
    'pbkdf2-sha256$210000$bqDT0xEisYWTW0jVh9HE6A==$5GCR7nC6ff07gMr+3X0IjG9ygHO0jnI4l1v3m2WpsQw=',
    'Demo Guest',
    NULL,
    'GUEST'
);



-- Sample reservations covering all four statuses.
INSERT INTO Reservation
    (guest_id, room_id, check_in, check_out, status, total_price)
SELECT
    u.user_id,
    rm.room_id,
    DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY),
    DATE_ADD(CURRENT_DATE, INTERVAL 33 DAY),
    sample.status,
    rt.price_per_night * 3
FROM (
    SELECT '101' AS room_number, 'PENDING' AS status
    UNION ALL
    SELECT '102', 'APPROVED'
    UNION ALL
    SELECT '201', 'CONFIRMED'
    UNION ALL
    SELECT '202', 'CANCELLED'
) AS sample
JOIN Room rm ON rm.room_number = sample.room_number
JOIN RoomType rt ON rt.type_id = rm.type_id
JOIN `User` u ON u.email = 'guest@example.com';



-- Sample payment for the confirmed demo booking.
-- Run this seed script before installing payment triggers.
INSERT INTO Payment (reservation_id, amount, payment_status)
SELECT r.reservation_id, r.total_price, 'PAID'
FROM Reservation r
JOIN `User` u ON u.user_id = r.guest_id
JOIN Room rm ON rm.room_id = r.room_id
WHERE u.email = 'guest@example.com'
  AND rm.room_number = '201'
  AND r.status = 'CONFIRMED';