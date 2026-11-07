# DAO and local database setup

`DBConnection` expects two Tomcat-managed JNDI DataSources:

- `java:comp/env/jdbc/HotelGuestDB`
- `java:comp/env/jdbc/HotelAdminDB`

They point to the `hotel_reservation` MySQL database using separate least-privilege DB accounts (`hotel_guest` and `hotel_admin`). When running standalone tests outside Tomcat (e.g., via `main()` in Eclipse), `DBConnection` automatically falls back to a local `DriverManager` connection targeting `jdbc:mysql://localhost:3306/hotel_reservation`.

The role in an HTTP session is enforced by `RoleFilter`; DAO ownership queries and cancellation/payment flows also rely on the guest id from the session, never a guest id supplied by a form.

## Setup steps

1. Install MySQL Connector/J in Tomcat 10.1's `lib` directory and restart Tomcat.
2. Copy `src/main/webapp/META-INF/context.xml.example` to `context.xml` in `src/main/webapp/META-INF/`.
3. Set the local database name (`hotel_reservation`) and both account credentials in `context.xml`. Never commit the resulting `context.xml`.
4. Apply Role 2's schema (`sql/schema.sql`), account grants, and seed rows, followed by Role 3's triggers and stored procedures (`sql/stored_procedures.sql`), before running DAO features.

## Schema and routine alignment

SQL mappings and exact procedure signatures are specified in `docs/PROJECT_RULES.md`. The current DAO implementation aligns with:

- `User`: `name`, `email`, `phone`, `password_hash`, `role`
- `RoomType` & `Room`: pricing/capacity on `RoomType`, room inventory on `Room`, and `Available_Rooms_View`
- `Reservation`: `guest_id`, `total_price` quote snapshot, and 7-argument `sp_create_reservation` / 3-argument `sp_cancel_reservation` calls
- `Payment`: `amount`, `refund_amount`, and `payment_status` (`PAID`, `PARTIALLY_REFUNDED`, `REFUNDED`, `VOID`), with `payment_method` omitted
