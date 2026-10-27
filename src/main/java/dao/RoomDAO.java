package dao;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Room inventory and RoomType pricing queries. */
public class RoomDAO {
    public record RoomRow(long roomId, String roomNumber, long typeId, String roomTypeName,
                          int floor, int capacity, BigDecimal pricePerNight) { }

    /** Date-specific overlap filtering is parameterized here; the view supplies room/type data. */
    public List<RoomRow> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut, Long typeId)
            throws SQLException {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }
        if (typeId != null && typeId <= 0) throw new IllegalArgumentException("typeId must be positive.");
        String sql = "SELECT v.room_id, v.room_number, v.type_id, v.room_type_name, v.floor, "
                + "v.capacity, v.price_per_night FROM Available_Rooms_View v "
                + "WHERE (? IS NULL OR v.type_id = ?) AND NOT EXISTS ("
                + "SELECT 1 FROM Reservation r WHERE r.room_id = v.room_id "
                + "AND r.status IN ('PENDING','APPROVED','CONFIRMED') "
                + "AND r.check_in < ? AND r.check_out > ?)";
        return DBConnection.query(DBConnection.AppRole.GUEST, sql, ps -> {
            if (typeId == null) {
                ps.setNull(1, java.sql.Types.BIGINT);
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(1, typeId);
                ps.setLong(2, typeId);
            }
            ps.setDate(3, java.sql.Date.valueOf(checkOut));
            ps.setDate(4, java.sql.Date.valueOf(checkIn));
        }, rs -> new RoomRow(rs.getLong("room_id"), rs.getString("room_number"),
                rs.getLong("type_id"), rs.getString("room_type_name"), rs.getInt("floor"),
                rs.getInt("capacity"), rs.getBigDecimal("price_per_night")));
    }

    public List<RoomRow> findAllRooms() throws SQLException {
        String sql = "SELECT rm.room_id, rm.room_number, rm.type_id, rt.name AS room_type_name, "
                + "rm.floor, rt.capacity, rt.price_per_night FROM Room rm "
                + "JOIN RoomType rt ON rt.type_id = rm.type_id ORDER BY rm.room_number";
        return DBConnection.query(DBConnection.AppRole.ADMIN, sql, null,
                rs -> new RoomRow(rs.getLong("room_id"), rs.getString("room_number"),
                        rs.getLong("type_id"), rs.getString("room_type_name"), rs.getInt("floor"),
                        rs.getInt("capacity"), rs.getBigDecimal("price_per_night")));
    }
    public boolean addRoom(String roomNumber, long typeId, int floor) throws SQLException {
        validateRoom(roomNumber, typeId);
        String sql = "INSERT INTO Room (room_number, type_id, floor) VALUES (?, ?, ?)";
        return DBConnection.update(DBConnection.AppRole.ADMIN, sql, ps -> {
            ps.setString(1, roomNumber.trim());
            ps.setLong(2, typeId);
            ps.setInt(3, floor);
        }) == 1;
    }

    public boolean updateRoom(long roomId, String roomNumber, long typeId, int floor)
            throws SQLException {
        if (roomId <= 0) throw new IllegalArgumentException("roomId must be positive.");
        validateRoom(roomNumber, typeId);
        String sql = "UPDATE Room SET room_number = ?, type_id = ?, floor = ? WHERE room_id = ?";
        return DBConnection.update(DBConnection.AppRole.ADMIN, sql, ps -> {
            ps.setString(1, roomNumber.trim());
            ps.setLong(2, typeId);
            ps.setInt(3, floor);
            ps.setLong(4, roomId);
        }) == 1;
    }

    public boolean deleteRoom(long roomId) throws SQLException {
        if (roomId <= 0) throw new IllegalArgumentException("roomId must be positive.");
        return DBConnection.update(DBConnection.AppRole.ADMIN, "DELETE FROM Room WHERE room_id = ?",
                ps -> ps.setLong(1, roomId)) == 1;
    }

    public boolean updateRoomTypePrice(long typeId, BigDecimal pricePerNight) throws SQLException {
        if (typeId <= 0) throw new IllegalArgumentException("typeId must be positive.");
        if (pricePerNight == null || pricePerNight.signum() < 0) {
            throw new IllegalArgumentException("pricePerNight cannot be negative.");
        }
        return DBConnection.update(DBConnection.AppRole.ADMIN,
                "UPDATE RoomType SET price_per_night = ? WHERE type_id = ?", ps -> {
                    ps.setBigDecimal(1, pricePerNight);
                    ps.setLong(2, typeId);
                }) == 1;
    }

    private static void validateRoom(String roomNumber, long typeId) {
        if (roomNumber == null || roomNumber.isBlank()) throw new IllegalArgumentException("roomNumber is required.");
        if (typeId <= 0) throw new IllegalArgumentException("typeId must be positive.");
    }
}
