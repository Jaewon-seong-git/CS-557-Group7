# DAO and local database setup

`DBConnection` expects two Tomcat-managed JNDI DataSources:

- `java:comp/env/jdbc/HotelGuestDB`
- `java:comp/env/jdbc/HotelAdminDB`

They point to the same MySQL database using separate least-privilege DB accounts. The role in an HTTP session is still enforced by `RoleFilter`; DAO ownership queries also use the guest id from the session, never a guest id supplied by a form.

1. Install MySQL Connector/J in Tomcat 10.1's `lib` directory and restart Tomcat.
2. Copy `src/main/webapp/META-INF/context.xml.example` to `context.xml` in the same folder.
3. Set the local database name and both account credentials. Never commit the resulting `context.xml`.
4. Apply Role 2's schema/account grants and Role 3's triggers/procedures before running DAO features.

SQL mappings and exact procedure signatures are specified in `docs/PROJECT_RULES.md`. The current DAO code follows `User.name`, `RoomType` pricing/capacity, `Room` inventory, `Reservation.guest_id/total_price`, and Payment status/refund fields.
