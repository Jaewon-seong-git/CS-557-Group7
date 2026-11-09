
-- COMPSCI 557 Group 7
-- Role 3: Database Programming
-- MySQL 8.0+
-- Database: hotel_reservation

USE hotel_reservation;

DROP TRIGGER IF EXISTS trg_reservation_before_insert;
DROP TRIGGER IF EXISTS trg_payment_after_insert;

DROP PROCEDURE IF EXISTS sp_create_reservation;
DROP PROCEDURE IF EXISTS sp_cancel_reservation;
DROP PROCEDURE IF EXISTS sp_expire_overdue;

DELIMITER $$

-- ============================================
-- TRIGGER 1: PREVENT OVERLAPPING RESERVATIONS
-- ============================================

CREATE TRIGGER trg_reservation_before_insert
BEFORE INSERT ON Reservation
FOR EACH ROW
BEGIN
    IF NEW.check_in IS NULL
       OR NEW.check_out IS NULL
       OR NEW.check_out <= NEW.check_in THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Check-out must be after check-in';
    END IF;

    IF DATEDIFF(NEW.check_in, CURRENT_DATE()) < 7 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Check-in must be at least 7 days away';
    END IF;

    IF NEW.status IN ('PENDING','APPROVED','CONFIRMED')
       AND EXISTS (
           SELECT 1
           FROM Reservation r
           WHERE r.room_id = NEW.room_id
             AND r.status IN ('PENDING','APPROVED','CONFIRMED')
             AND r.check_in < NEW.check_out
             AND r.check_out > NEW.check_in
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Room already booked for these dates';
    END IF;
END$$

-- ============================================
-- PROCEDURE 1: CREATE RESERVATION
-- ============================================

CREATE PROCEDURE sp_create_reservation(
    IN p_guest_id BIGINT,
    IN p_room_id BIGINT,
    IN p_check_in DATE,
    IN p_check_out DATE,
    OUT p_reservation_id BIGINT,
    OUT p_total_price DECIMAL(10,2),
    OUT p_status_msg VARCHAR(255)
)
BEGIN
    DECLARE v_room_id BIGINT DEFAULT NULL;
    DECLARE v_rate DECIMAL(10,2) DEFAULT NULL;
    DECLARE v_guest_count INT DEFAULT 0;
    DECLARE v_conflicts INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_reservation_id = NULL;
    SET p_total_price = NULL;
    SET p_status_msg = NULL;

    IF p_guest_id IS NULL OR p_guest_id <= 0
       OR p_room_id IS NULL OR p_room_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Guest and room IDs must be positive';
    END IF;

    IF p_check_in IS NULL OR p_check_out IS NULL
       OR p_check_out <= p_check_in THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Check-out must be after check-in';
    END IF;

    IF DATEDIFF(p_check_in, CURRENT_DATE()) < 7 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Check-in must be at least 7 days away';
    END IF;

    START TRANSACTION;

    -- Lock the room to serialize same-room bookings.
    SELECT rm.room_id, rt.price_per_night
    INTO v_room_id, v_rate
    FROM Room rm
    JOIN RoomType rt ON rt.type_id = rm.type_id
    WHERE rm.room_id = p_room_id
    FOR UPDATE;

    IF v_room_id IS NULL OR v_rate IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Room does not exist or has no rate';
    END IF;

    SELECT COUNT(*)
    INTO v_guest_count
    FROM `User`
    WHERE user_id = p_guest_id
      AND role = 'GUEST';

    IF v_guest_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Guest does not exist or is not a guest';
    END IF;

    SELECT COUNT(*)
    INTO v_conflicts
    FROM Reservation
    WHERE room_id = p_room_id
      AND status IN ('PENDING','APPROVED','CONFIRMED')
      AND check_in < p_check_out
      AND check_out > p_check_in;

    IF v_conflicts > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Room is unavailable for these dates';
    END IF;

    SET p_total_price =
        v_rate * DATEDIFF(p_check_out, p_check_in);

    INSERT INTO Reservation (
        guest_id,
        room_id,
        check_in,
        check_out,
        status,
        total_price
    )
    VALUES (
        p_guest_id,
        p_room_id,
        p_check_in,
        p_check_out,
        'PENDING',
        p_total_price
    );

    SET p_reservation_id = LAST_INSERT_ID();
    SET p_status_msg = 'SUCCESS';

    COMMIT;
END$$

-- ============================================
-- TRIGGER 2: VERIFY PAYMENT AND CONFIRM
-- ============================================

CREATE TRIGGER trg_payment_after_insert
AFTER INSERT ON Payment
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(10) DEFAULT NULL;
    DECLARE v_check_in DATE DEFAULT NULL;
    DECLARE v_total DECIMAL(10,2) DEFAULT NULL;

    SELECT r.status, r.check_in, r.total_price
    INTO v_status, v_check_in, v_total
    FROM Reservation r
    WHERE r.reservation_id = NEW.reservation_id;

    IF v_status IS NULL OR v_status <> 'APPROVED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Payment requires APPROVED reservation';
    END IF;

    IF DATEDIFF(v_check_in, CURRENT_DATE()) < 7 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Payment requires 7-day advance booking';
    END IF;

    IF NEW.payment_status IS NULL
       OR NEW.payment_status <> 'PAID'
       OR NEW.amount IS NULL
       OR v_total IS NULL
       OR NEW.amount <> v_total THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Payment must be PAID for full reservation total';
    END IF;

    UPDATE Reservation
    SET status = 'CONFIRMED'
    WHERE reservation_id = NEW.reservation_id
      AND status = 'APPROVED';
END$$

-- ============================================
-- PROCEDURE 2: CANCEL RESERVATION
-- ============================================
-- Guest ownership must be verified in Java
-- before calling this procedure.

CREATE PROCEDURE sp_cancel_reservation(
    IN p_reservation_id BIGINT,
    OUT p_refund_amount DECIMAL(10,2),
    OUT p_status_msg VARCHAR(255)
)
BEGIN
    DECLARE v_check_in DATE DEFAULT NULL;
    DECLARE v_status VARCHAR(10) DEFAULT NULL;
    DECLARE v_days INT DEFAULT NULL;
    DECLARE v_payment_id BIGINT DEFAULT NULL;
    DECLARE v_paid DECIMAL(10,2) DEFAULT NULL;
    DECLARE v_payment_status VARCHAR(20) DEFAULT NULL;
    DECLARE v_prior_refund DECIMAL(10,2) DEFAULT 0.00;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_refund_amount = 0.00;
    SET p_status_msg = NULL;

    IF p_reservation_id IS NULL OR p_reservation_id <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Reservation ID must be positive';
    END IF;

    START TRANSACTION;

    SELECT check_in, status
    INTO v_check_in, v_status
    FROM Reservation
    WHERE reservation_id = p_reservation_id
    FOR UPDATE;

    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Reservation not found';
    END IF;

    IF v_status = 'CANCELLED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Reservation already cancelled';
    END IF;

    SET v_days = DATEDIFF(v_check_in, CURRENT_DATE());

    IF v_days <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot cancel on or after check-in';
    END IF;

    -- Payment may not exist for unpaid reservations.
    SELECT payment_id,
           amount,
           payment_status,
           COALESCE(refund_amount, 0)
    INTO v_payment_id,
         v_paid,
         v_payment_status,
         v_prior_refund
    FROM Payment
    WHERE reservation_id = p_reservation_id
    FOR UPDATE;

    IF v_payment_id IS NOT NULL THEN

        IF v_payment_status <> 'PAID'
           OR v_prior_refund <> 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Payment already refunded or not eligible';
        END IF;

        SET p_refund_amount = CASE
            WHEN v_days > 7 THEN v_paid
            WHEN v_days BETWEEN 3 AND 7
                THEN ROUND(v_paid * 0.50, 2)
            ELSE 0.00
        END;

        UPDATE Payment
        SET refund_amount = p_refund_amount,
            refunded_at = CASE
                WHEN p_refund_amount > 0
                    THEN CURRENT_TIMESTAMP
                ELSE NULL
            END,
            payment_status = CASE
                WHEN p_refund_amount = v_paid
                    THEN 'REFUNDED'
                WHEN p_refund_amount > 0
                    THEN 'PARTIALLY_REFUNDED'
                ELSE 'PAID'
            END
        WHERE payment_id = v_payment_id;

    END IF;

    UPDATE Reservation
    SET status = 'CANCELLED'
    WHERE reservation_id = p_reservation_id;

    SET p_status_msg = 'SUCCESS';

    COMMIT;
END$$

-- ============================================
-- PROCEDURE 3: EXPIRE OVERDUE BOOKINGS
-- ============================================

CREATE PROCEDURE sp_expire_overdue(
    OUT p_cancelled_count INT
)
BEGIN
    DECLARE v_done BOOLEAN DEFAULT FALSE;
    DECLARE v_reservation_id BIGINT;
    DECLARE v_changed INT DEFAULT 0;

    DECLARE cur_expire CURSOR FOR
        SELECT r.reservation_id
        FROM Reservation r
        WHERE r.status IN ('PENDING','APPROVED')
          AND DATEDIFF(r.check_in, CURRENT_DATE()) < 7
          AND NOT EXISTS (
              SELECT 1
              FROM Payment p
              WHERE p.reservation_id = r.reservation_id
          );

    DECLARE CONTINUE HANDLER FOR NOT FOUND
        SET v_done = TRUE;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_cancelled_count = 0;

    START TRANSACTION;

    OPEN cur_expire;

    expiry_loop: LOOP

        FETCH cur_expire INTO v_reservation_id;

        IF v_done THEN
            LEAVE expiry_loop;
        END IF;

        UPDATE Reservation r
        SET r.status = 'CANCELLED'
        WHERE r.reservation_id = v_reservation_id
          AND r.status IN ('PENDING','APPROVED')
          AND DATEDIFF(r.check_in, CURRENT_DATE()) < 7
          AND NOT EXISTS (
              SELECT 1
              FROM Payment p
              WHERE p.reservation_id = r.reservation_id
          );

        SET v_changed = ROW_COUNT();

        SET p_cancelled_count =
            p_cancelled_count + v_changed;

    END LOOP;

    CLOSE cur_expire;

    COMMIT;
END$$

DELIMITER ;