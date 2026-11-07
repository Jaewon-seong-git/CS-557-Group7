# Hotel Reservation Management System

Eclipse Dynamic Web Project for COMPSCI 557 Group 7. The implementation baseline is Java 17, Jakarta Servlet 6, Tomcat 10.1, and MySQL 8.0 or later.

## Project layout

- `src/main/java/dao`: database connection (`DBConnection.java`) and DAO operations (`UserDAO`, `RoomDAO`, `ReservationDAO`)
- `src/main/java/servlet`: registration, login, and logout endpoints
- `src/main/java/filter`: guest/admin route authorization (`RoleFilter`)
- `src/main/java/util`: shared input validation
- `src/main/webapp`: web resources; JSP views belong under `WEB-INF/views`
- `sql`: Role 2 and Role 3 deliverables (`schema.sql`, `stored_procedures.sql`)
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

1. Run `sql/schema.sql` to create the `hotel_reservation` database, tables, indexes, and views, and configure the two least-privilege MySQL accounts (`hotel_admin`, `hotel_guest`) described in `docs/PROJECT_RULES.md`.
2. Put MySQL Connector/J in Tomcat 10.1's `lib` folder and restart Tomcat.
3. Copy `src/main/webapp/META-INF/context.xml.example` to `context.xml` in that folder.
4. Set the local DB name (`hotel_reservation`) and each account's credentials. Keep real credentials only in the ignored `context.xml`.
5. In Eclipse right-click the project, choose **Run As > Run on Server**, then select **Tomcat v10.1**.

## Current integration status

- **Role 1 (PM / Core Backend):** Core DAOs (`DBConnection`, `UserDAO`, `RoomDAO`, `ReservationDAO`), authentication, session filter, and `docs/PROJECT_RULES.md` aligned with the `hotel_reservation` schema.
- **Role 2 (DB Schema):** Initial `sql/schema.sql` (5 tables, constraints, indexes, and views) merged; sample seed data and account `GRANT` scripts in progress.
- **Role 3 / Role 4 / Role 5:** Stored procedures/triggers (`stored_procedures.sql`) and Admin/Guest JSP & servlet pages are pending integration. Full end-to-end runtime verification requires all role deliverables to be merged.

## Team contribution flow

Use one branch per role/task and open a pull request to `main`. Before changing a shared DB column, route, session key, or procedure signature, update `docs/PROJECT_RULES.md` and notify the affected role owners.
