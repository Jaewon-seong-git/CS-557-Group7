# Hotel Reservation Management System

Eclipse Dynamic Web Project for COMPSCI 557 Group 7. The implementation baseline is Java 17, Jakarta Servlet 6, Tomcat 10.1, and MySQL 8.0 or later.

## Project layout

- `src/main/java/dao`: database connection (`DBConnection.java`) and DAO operations (`UserDAO`, `RoomDAO`, `ReservationDAO`)
- `src/main/java/servlet`: authentication (`Login`, `Logout`, `Registration`) and Admin/Guest endpoints
- `src/main/java/filter`: guest/admin route authorization (`RoleFilter`)
- `src/main/java/util`: shared input validation
- `src/main/webapp`: web resources; JSP views belong under `WEB-INF/views`
- `sql`: Role 2 and Role 3 deliverables (`schema.sql`, `seed.sql`, `stored_procedures.sql`, `grants.sql`)
- `docs/PROJECT_RULES.md`: PM implementation baseline and cross-role contracts
- `src/main/webapp/META-INF/context.xml.example`: local Tomcat JNDI pool template

## Open in Eclipse

1. Clone the shared repository to a local folder.
2. In Eclipse choose **File > Import > General > Existing Projects into Workspace**.
3. Select the cloned repository root and import `HotelReservationSystem`.
4. In Project Explorer, right-click the project and choose **Refresh**. Expand `Java Resources > src/main/java`.
5. Select **Project > Clean** if Eclipse reports stale build output.

Keep `.project`, `.classpath`, and `.settings` in Git so teammates get the same Eclipse project setup. Do not commit the Eclipse workspace `.metadata` folder.

## Local database and Tomcat setup

1. Run the SQL scripts in MySQL **in this exact order** (seed data must be inserted before payment triggers are installed):
   1. `sql/schema.sql` (creates `hotel_reservation` database, tables, indexes, and views)
   2. `sql/seed.sql` (inserts sample room types, rooms, demo users, and demo reservations/payment)
   3. `sql/stored_procedures.sql` (installs triggers and stored procedures)
   4. `sql/grants.sql` (creates/configures least-privilege `hotel_admin` and `hotel_guest` accounts and grants)
2. Put MySQL Connector/J in Tomcat 10.1's `lib` folder and restart Tomcat.
3. Copy `src/main/webapp/META-INF/context.xml.example` to `context.xml` in that folder.
4. Set the local DB name (`hotel_reservation`) and each account's credentials. Keep real credentials only in the ignored `context.xml`.
5. In Eclipse right-click the project, choose **Run As > Run on Server**, then select **Tomcat v10.1**.

## Current integration status

- **Role 1 (PM / Core Backend):** Core DAOs (`DBConnection`, `UserDAO`, `RoomDAO`, `ReservationDAO`), authentication, `RoleFilter`, and `docs/PROJECT_RULES.md` merged and aligned with `hotel_reservation`.
- **Role 2 (DB Schema):** `schema.sql`, `seed.sql`, and `grants.sql` merged.
- **Role 3 (DB Logic):** `stored_procedures.sql` (2 triggers, 3 procedures including expiry cursor) merged.
- **Role 4 (Admin UI) & Role 5 (Guest UI):** Phase 1 Admin JSP/Servlets and Guest profile JSP/Servlet merged.
- **Next Phase:** End-to-End Integration, Guest booking/payment/cancellation UI completion, and boundary/security testing.

## Team contribution flow

Use one branch per role/task and open a pull request to `main`. Before changing a shared DB column, route, session key, or procedure signature, update `docs/PROJECT_RULES.md` and notify the affected role owners.
