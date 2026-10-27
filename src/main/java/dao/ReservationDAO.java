package dao;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

/** Reservation, payment, cancellation, and guest booking-history operations. */
public class ReservationDAO {
    public record ReservationReceipt(long reservationId, BigDecimal totalPrice) { }
    public record BookingRow(long reservationId, long roomId, LocalDate checkIn,
                             LocalDate checkOut, String status, BigDecimal totalPrice) { }
    public record CancellationResult(BigDecimal refundAmount) { }

    /** Procedure contract: four IN arguments, then OUT reservation id and quoted total. */
    public ReservationReceipt createReservation(long guestId, long roomId,
                                                LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        validateStay(guestId, roomId, checkIn, checkOut);
        String call = "{call sp_create_reservation(?, ?, ?, ?, ?, ?)}";
        try (var connection = DBConnection.getConnection(DBConnection.AppRole.GUEST);
             CallableStatement statement = connection.prepareCall(call)) {
            statement.setLong(1, guestId);
            statement.setLong(2, roomId);
            statement.setDate(3, java.sql.Date.valueOf(checkIn));
            statement.setDate(4, java.sql.Date.valueOf(checkOut));
            statement.registerOutParameter(5, Types.BIGINT);
            statement.registerOutParameter(6, Types.DECIMAL);
            statement.execute();
            long id = statement.getLong(5);
            BigDecimal total = statement.getBigDecimal(6);
            if (statement.wasNull() || id <= 0 || total == null) {
                throw new SQLException("sp_create_reservation did not return the reservation receipt.");
            }
            return new ReservationReceipt(id, total);
        }
    }

    /** The database trigger validates APPROVED status and the seven-day payment deadline. */
    public boolean recordPayment(long reservationId, long guestId) throws SQLException {
        if (reservationId <= 0 || guestId <= 0) throw new IllegalArgumentException("IDs must be positive.");
        String sql = "INSERT INTO Payment (reservation_id, amount, paid_at, payment_status) "
                + "SELECT r.reservation_id, r.total_price, CURRENT_TIMESTAMP, 'PAID' FROM Reservation r "
                + "WHERE r.reservation_id = ? AND r.guest_id = ? AND r.status = 'APPROVED'";
        return DBConnection.update(DBConnection.AppRole.GUEST, sql, ps -> {
            ps.setLong(1, reservationId);
            ps.setLong(2, guestId);
        }) == 1;
    }

    /** The procedure verifies ownership and returns the computed refund amount. */
    public CancellationResult cancelReservation(long reservationId, long guestId) throws SQLException {
        if (reservationId <= 0 || guestId <= 0) throw new IllegalArgumentException("IDs must be positive.");
        try (var connection = DBConnection.getConnection(DBConnection.AppRole.GUEST);
             CallableStatement statement = connection.prepareCall("{call sp_cancel_reservation(?, ?, ?)}")) {
            statement.setLong(1, reservationId);
            statement.setLong(2, guestId);
            statement.registerOutParameter(3, Types.DECIMAL);
            statement.execute();
            return new CancellationResult(statement.getBigDecimal(3));
        }
    }

    public List<BookingRow> findBookingsForGuest(long guestId) throws SQLException {
        if (guestId <= 0) throw new IllegalArgumentException("guestId must be positive.");
        String sql = "SELECT r.reservation_id, r.room_id, r.check_in, r.check_out, r.status, "
                + "COALESCE(p.amount, r.total_price) AS total_price FROM Reservation r "
                + "LEFT JOIN Payment p ON p.reservation_id = r.reservation_id "
                + "WHERE r.guest_id = ? ORDER BY r.check_in DESC";
        return DBConnection.query(DBConnection.AppRole.GUEST, sql, ps -> ps.setLong(1, guestId),
                rs -> new BookingRow(rs.getLong("reservation_id"), rs.getLong("room_id"),
                        rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                        rs.getString("status"), rs.getBigDecimal("total_price")));
    }

    public record AdminReservationRow(long reservationId, long guestId, String guestName,
                                      long roomId, String roomNumber, LocalDate checkIn,
                                      LocalDate checkOut, String status, BigDecimal totalPrice) { }

    public List<AdminReservationRow> findReservationsForAdmin(String status) throws SQLException {
        if (status != null && !List.of("PENDING", "APPROVED", "CONFIRMED", "CANCELLED")
                .contains(status.toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported reservation status.");
        }
        String sql = "SELECT r.reservation_id, r.guest_id, u.name AS guest_name, r.room_id, "
                + "rm.room_number, r.check_in, r.check_out, r.status, r.total_price "
                + "FROM Reservation r JOIN `User` u ON u.user_id = r.guest_id "
                + "JOIN Room rm ON rm.room_id = r.room_id "
                + "WHERE (? IS NULL OR r.status = ?) ORDER BY r.created_at DESC";
        return DBConnection.query(DBConnection.AppRole.ADMIN, sql, ps -> {
            if (status == null) {
                ps.setNull(1, Types.VARCHAR);
                ps.setNull(2, Types.VARCHAR);
            } else {
                String normalized = status.toUpperCase(java.util.Locale.ROOT);
                ps.setString(1, normalized);
                ps.setString(2, normalized);
            }
        }, rs -> new AdminReservationRow(rs.getLong("reservation_id"), rs.getLong("guest_id"),
                rs.getString("guest_name"), rs.getLong("room_id"), rs.getString("room_number"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getString("status"), rs.getBigDecimal("total_price")));
    }

    public boolean approveReservation(long reservationId) throws SQLException {
        if (reservationId <= 0) throw new IllegalArgumentException("reservationId must be positive.");
        String sql = "UPDATE Reservation SET status = 'APPROVED' "
                + "WHERE reservation_id = ? AND status = 'PENDING'";
        return DBConnection.update(DBConnection.AppRole.ADMIN, sql,
                ps -> ps.setLong(1, reservationId)) == 1;
    }

    /** Controlled administrative override; payment state is derived from the Payment row. */
    public boolean adminSetReservationStatus(long reservationId, String status) throws SQLException {
        if (reservationId <= 0) throw new IllegalArgumentException("reservationId must be positive.");
        String normalized = status == null ? "" : status.toUpperCase(java.util.Locale.ROOT);
        if (!List.of("PENDING", "APPROVED", "CONFIRMED", "CANCELLED").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported reservation status.");
        }
        String sql = "UPDATE Reservation SET status = ? WHERE reservation_id = ?";
        return DBConnection.update(DBConnection.AppRole.ADMIN, sql, ps -> {
            ps.setString(1, normalized);
            ps.setLong(2, reservationId);
        }) == 1;
    }
    /** Admin endpoint for the cursor-based unpaid-booking expiry procedure. */
    public int expireOverdueReservations() throws SQLException {
        try (var connection = DBConnection.getConnection(DBConnection.AppRole.ADMIN);
             CallableStatement statement = connection.prepareCall("{call sp_expire_overdue(?)}")) {
            statement.registerOutParameter(1, Types.INTEGER);
            statement.execute();
            return statement.getInt(1);
        }
    }
    /** Admin override keeps payment status and refund totals internally consistent. */
    public boolean adminOverridePayment(long reservationId, String paymentStatus,
                                        BigDecimal refundAmount) throws SQLException {
        if (reservationId <= 0) throw new IllegalArgumentException("reservationId must be positive.");
        String normalized = paymentStatus == null ? "" : paymentStatus.toUpperCase(java.util.Locale.ROOT);
        if (!List.of("PAID", "PARTIALLY_REFUNDED", "REFUNDED", "VOID").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported payment status.");
        }
        if (refundAmount == null || refundAmount.signum() < 0) {
            throw new IllegalArgumentException("refundAmount cannot be negative.");
        }
        try (var connection = DBConnection.getConnection(DBConnection.AppRole.ADMIN)) {
            connection.setAutoCommit(false);
            try (var select = connection.prepareStatement(
                    "SELECT amount FROM Payment WHERE reservation_id = ? FOR UPDATE")) {
                select.setLong(1, reservationId);
                try (var result = select.executeQuery()) {
                    if (!result.next()) return false;
                    BigDecimal amount = result.getBigDecimal("amount");
                    int comparison = refundAmount.compareTo(amount);
                    boolean valid = switch (normalized) {
                        case "PAID", "VOID" -> refundAmount.signum() == 0;
                        case "PARTIALLY_REFUNDED" -> refundAmount.signum() > 0 && comparison < 0;
                        case "REFUNDED" -> comparison == 0;
                        default -> false;
                    };
                    if (!valid) throw new IllegalArgumentException("Payment status and refund amount do not match.");
                    try (var update = connection.prepareStatement(
                            "UPDATE Payment SET payment_status = ?, refund_amount = ?, "
                            + "refunded_at = CASE WHEN ? = 0 THEN NULL ELSE CURRENT_TIMESTAMP END "
                            + "WHERE reservation_id = ?")) {
                        update.setString(1, normalized);
                        update.setBigDecimal(2, refundAmount);
                        update.setBigDecimal(3, refundAmount);
                        update.setLong(4, reservationId);
                        boolean changed = update.executeUpdate() == 1;
                        connection.commit();
                        return changed;
                    }
                }
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
    private static void validateStay(long guestId, long roomId, LocalDate checkIn, LocalDate checkOut) {
        if (guestId <= 0 || roomId <= 0) throw new IllegalArgumentException("IDs must be positive.");
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }
    }
}
