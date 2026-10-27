package dao;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Authentication and profile queries for User. */
public class UserDAO {
    private static final int HASH_ITERATIONS = 210_000;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Credentials(long userId, String name, String email,
                              String passwordHash, String role) { }

    public record GuestProfile(long userId, String name, String email, String phone) { }

    public Credentials findCredentialsByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) return null;
        String sql = "SELECT user_id, name, email, password_hash, role FROM `User` WHERE email = ?";
        List<Credentials> rows = DBConnection.query(DBConnection.AppRole.GUEST, sql,
                ps -> ps.setString(1, email.trim()),
                rs -> new Credentials(rs.getLong("user_id"), rs.getString("name"),
                        rs.getString("email"), rs.getString("password_hash"), rs.getString("role")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Public self-registration always creates a GUEST; it cannot grant ADMIN. */
    public long registerGuest(String name, String email, String phone, char[] password)
            throws SQLException {
        requireText(name, "name");
        requireText(email, "email");
        requirePassword(password);
        String sql = "INSERT INTO `User` (name, email, phone, password_hash, role) "
                + "VALUES (?, ?, ?, ?, 'GUEST')";
        String encoded = hashPassword(password);
        try (var connection = DBConnection.getConnection(DBConnection.AppRole.GUEST);
             var statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name.trim());
            statement.setString(2, email.trim());
            statement.setString(3, phone == null || phone.isBlank() ? null : phone.trim());
            statement.setString(4, encoded);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("User insert succeeded but no user_id was returned.");
            }
        }
    }

    public boolean updateProfile(long userId, String name, String email, String phone)
            throws SQLException {
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive.");
        requireText(name, "name");
        requireText(email, "email");
        String sql = "UPDATE `User` SET name = ?, email = ?, phone = ? WHERE user_id = ? AND role = 'GUEST'";
        return DBConnection.update(DBConnection.AppRole.GUEST, sql, ps -> {
            ps.setString(1, name.trim());
            ps.setString(2, email.trim());
            ps.setString(3, phone == null || phone.isBlank() ? null : phone.trim());
            ps.setLong(4, userId);
        }) == 1;
    }

    /** Admin directory query excludes authentication secrets. */
    public List<GuestProfile> findAllGuests() throws SQLException {
        String sql = "SELECT user_id, name, email, phone FROM `User` WHERE role = 'GUEST' ORDER BY name";
        return DBConnection.query(DBConnection.AppRole.ADMIN, sql, null,
                rs -> new GuestProfile(rs.getLong("user_id"), rs.getString("name"),
                        rs.getString("email"), rs.getString("phone")));
    }

    public boolean adminUpdateGuestProfile(long userId, String name, String email, String phone)
            throws SQLException {
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive.");
        requireText(name, "name");
        requireText(email, "email");
        String sql = "UPDATE `User` SET name = ?, email = ?, phone = ? WHERE user_id = ? AND role = 'GUEST'";
        return DBConnection.update(DBConnection.AppRole.ADMIN, sql, ps -> {
            ps.setString(1, name.trim());
            ps.setString(2, email.trim());
            ps.setString(3, phone == null || phone.isBlank() ? null : phone.trim());
            ps.setLong(4, userId);
        }) == 1;
    }
    /** Format: pbkdf2-sha256$iterations$salt$derivedKey; salt and key are Base64. */
    public static String hashPassword(char[] password) throws SQLException {
        requirePassword(password);
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, HASH_ITERATIONS);
        return "pbkdf2-sha256$" + HASH_ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean verifyPassword(char[] candidate, String storedHash) {
        if (candidate == null || storedHash == null) return false;
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length < 16 || expected.length != HASH_BITS / 8) return false;
            return MessageDigest.isEqual(expected, derive(candidate, salt, iterations));
        } catch (IllegalArgumentException | SQLException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) throws SQLException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new SQLException("Password hashing is unavailable in this Java runtime.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static void requirePassword(char[] password) {
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
    }
}
