-- First-time local setup. Run each statement separately as a MySQL admin
-- and save the generated passwords for your local context.xml.
-- CREATE USER 'hotel_admin'@'localhost' IDENTIFIED BY RANDOM PASSWORD;
-- CREATE USER 'hotel_guest'@'localhost' IDENTIFIED BY RANDOM PASSWORD;


-- Admin: room inventory, pricing, bookings, refunds, and guest directory.
GRANT SELECT, INSERT, UPDATE, DELETE ON hotel_reservation.Room
    TO 'hotel_admin'@'localhost';
GRANT SELECT, UPDATE (price_per_night) ON hotel_reservation.RoomType
    TO 'hotel_admin'@'localhost';
GRANT SELECT, UPDATE (status) ON hotel_reservation.Reservation
    TO 'hotel_admin'@'localhost';
GRANT SELECT, UPDATE (refund_amount, payment_status) ON hotel_reservation.Payment
    TO 'hotel_admin'@'localhost';
GRANT SELECT (user_id, name, email, phone, role), UPDATE (name, email, phone)
    ON hotel_reservation.`User` TO 'hotel_admin'@'localhost';
GRANT SELECT ON hotel_reservation.Active_Guest_Summary_View
    TO 'hotel_admin'@'localhost';

-- Guest: login, registration, profile, room search, bookings, and payment.
GRANT SELECT, INSERT (name, email, phone, password_hash, role),
    UPDATE (name, email, phone)
    ON hotel_reservation.`User` TO 'hotel_guest'@'localhost';
GRANT SELECT ON hotel_reservation.Available_Rooms_View
    TO 'hotel_guest'@'localhost';
GRANT SELECT ON hotel_reservation.Reservation
    TO 'hotel_guest'@'localhost';
GRANT SELECT (reservation_id, amount), INSERT (reservation_id, amount)
    ON hotel_reservation.Payment TO 'hotel_guest'@'localhost';

-- Uncomment these after Role 3's procedures are installed.
-- GRANT EXECUTE ON PROCEDURE hotel_reservation.sp_create_reservation
--     TO 'hotel_guest'@'localhost';
-- GRANT EXECUTE ON PROCEDURE hotel_reservation.sp_cancel_reservation
--     TO 'hotel_guest'@'localhost';
-- GRANT EXECUTE ON PROCEDURE hotel_reservation.sp_expire_overdue
--     TO 'hotel_admin'@'localhost';