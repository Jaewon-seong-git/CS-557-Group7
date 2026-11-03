package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/** JNDI access to the Tomcat-managed, least-privilege MySQL pools. */
public final class DBConnection {
    public enum AppRole { GUEST, ADMIN }

    private static final String GUEST_JNDI = "java:comp/env/jdbc/HotelGuestDB";
    private static final String ADMIN_JNDI = "java:comp/env/jdbc/HotelAdminDB";

    private DBConnection() { }

    public static Connection getConnection(AppRole role) throws SQLException {
        if (role == null) throw new IllegalArgumentException("Database role is required.");
        String name = role == AppRole.ADMIN ? ADMIN_JNDI : GUEST_JNDI;
        InitialContext context = null;
        try {
            context = new InitialContext();
            Object resource = context.lookup(name);
            if (!(resource instanceof DataSource dataSource)) {
                throw new SQLException("JNDI resource is not a DataSource: " + name);
            }
            return dataSource.getConnection();
        } catch (NamingException e) {
            throw new SQLException("Cannot find pooled database resource " + name
                    + ". Configure the Tomcat JNDI DataSource first.", e);
        } finally {
            if (context != null) {
                try { context.close(); }
                catch (NamingException ignored) { /* The pooled Connection is independently managed. */ }
            }
        }
    }

    @FunctionalInterface
    public interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    /** Executes a parameterized query and closes its JDBC resources. */
    public static <T> List<T> query(AppRole role, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        try (Connection connection = getConnection(role);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (resultSet.next()) rows.add(mapper.map(resultSet));
                return rows;
            }
        }
    }

    /** Executes a parameterized INSERT, UPDATE, or DELETE. */
    public static int update(AppRole role, String sql, Binder binder) throws SQLException {
        try (Connection connection = getConnection(role);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) binder.bind(statement);
            return statement.executeUpdate();
        }
    }

    public static Connection getConnection() throws SQLException {
    try {
        // Tomcat Server Environment
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        DataSource ds = (DataSource) envCtx.lookup("jdbc/HotelDB");
        return ds.getConnection();
    } catch (NamingException e) {
        // Eclipse Local Connection
        String url = "jdbc:mysql://localhost:3306/hotel_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = "hotel_admin";
        String pass = "AdminPass123!";
        return DriverManager.getConnection(url, user, pass);
    }
}
}
