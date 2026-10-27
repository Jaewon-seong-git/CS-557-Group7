# Project implementation rules

**PM implementation baseline** based on the group's proposal and internal execution plan. Use this as the shared source of truth for Phase 1 integration. If the instructor's rubric requires a different rule, update this file before changing code.

## Platform and naming

- Java 17, Jakarta Servlet 6.0, Tomcat 10.1, MySQL 8.0+.
- SQL table names: `User`, `RoomType`, `Room`, `Reservation`, `Payment`.
- IDs use `BIGINT`; monetary values use `DECIMAL(10,2)`; hotel stay dates use SQL `DATE` / Java `LocalDate`.
- Status spellings are uppercase and use `CANCELLED` (two Ls).
- Check-out must be later than check-in. Reservation dates are half-open intervals `[check_in, check_out)`. Two stays overlap iff `existing.check_in < requested.check_out AND existing.check_out > requested.check_in`.

## Schema contract for Role 2

Create five tables with these columns. Role 2 owns the DDL, exact SQL types, indexes, constraints, views, ERD, account grants, and seed rows.

- `User`: `user_id`, `email`, `password_hash`, `name`, `phone`, `role` (`ADMIN` or `GUEST`). Email is unique. Public registration always creates `GUEST`; admin accounts are seeded.
- `RoomType`: `type_id`, `name` (`Standard`, `Deluxe`, `Suite`), `description`, `capacity`, `price_per_night`. Capacity >= 1; price >= 0. Pricing and capacity belong here, not on Room.
- `Room`: `room_id`, unique `room_number`, `type_id` FK, `floor`.
- `Reservation`: `reservation_id`, `guest_id` FK to User, `room_id` FK to Room, `check_in`, `check_out`, `status`, `created_at`, and `total_price DECIMAL(10,2)` snapshot. The proposal omitted `total_price`, but it is required so a later RoomType price edit cannot change an existing reservation quote or payment amount.
- `Payment`: `payment_id`, unique `reservation_id` FK, `amount`, `payment_status`, `paid_at`, `refund_amount` default 0, `refunded_at`. The proposal lists payment timestamps and refund amount but not `payment_status`; this column is added so the explicitly required Admin payment-status override has a consistent stored value. Allowed payment statuses: `PAID`, `PARTIALLY_REFUNDED`, `REFUNDED`, `VOID`.

Reservation statuses are `PENDING`, `APPROVED`, `CONFIRMED`, and `CANCELLED`. The unique Payment reservation FK means an unpaid reservation has no Payment row yet; a paid reservation has at most one Payment row.

For deletion, preserve booking/payment history: Room and User rows with Reservations should use `RESTRICT`/`NO ACTION`; Payment may be deleted only as part of deleting a Reservation where permitted by the course's cascade requirement. Admin room deletion should return a clear constraint message when reservations exist; do not silently erase history.

Views:

- `Available_Rooms_View` publishes `room_id`, `room_number`, `type_id`, `room_type_name`, `floor`, `capacity`, `price_per_night`. It supplies room/type attributes; `RoomDAO` applies requested-date overlap filtering because a standard view cannot accept date parameters.
- `Active_Guest_Summary_View` returns one guest row with identity/contact fields and a count of reservations in `PENDING`, `APPROVED`, or `CONFIRMED`. Do not include password hashes.

Create two composite indexes: `(room_id, check_in, check_out)` for reservation overlap checks and `(status, check_in)` for pending/expiry selection. Seed the three room types, at least one Admin and Guest, and Reservation rows covering all four statuses. Store password hashes only.

## Status, payment, and cancellation rules

Reservation lifecycle:

```text
PENDING --Admin approves--> APPROVED --valid payment--> CONFIRMED
   |                            |                         |
   +---- cancel/expire ---------+---- cancel/expire ------+--> CANCELLED
```

- `PENDING`, `APPROVED`, and `CONFIRMED` block room availability. `CANCELLED` releases the room.
- A guest may request a reservation only if check-out is after check-in and check-in is at least 7 calendar days away. The same boundary applies to payment: `DATEDIFF(check_in, CURRENT_DATE) >= 7` is allowed; fewer than 7 days is not. UI text should say “at least 7 days before check-in,” not “8 days.”
- `sp_create_reservation` checks overlap, snapshots the current RoomType rate times `DATEDIFF(check_out, check_in)`, creates `PENDING`, and returns both the id and quoted total. The procedure locks the Room row while checking overlap to avoid concurrent double booking. The required BEFORE INSERT reservation trigger also rejects an overlap as a second integrity guard.
- Admin approval changes only `PENDING` to `APPROVED`.
- The payment trigger accepts a new Payment only for `APPROVED` with at least 7 days to check-in. It signals a SQL error otherwise; on success it changes Reservation to `CONFIRMED`.
- `sp_expire_overdue` uses a cursor over unpaid `PENDING`/`APPROVED` rows with fewer than 7 calendar days before check-in and changes them to `CANCELLED`. Repeated runs are safe and no refund is recorded for unpaid reservations.
- A guest may cancel before check-in. For paid reservations, use calendar days `DATEDIFF(check_in, CURRENT_DATE)`: more than 7 days = 100% refund; 3 through 7 days = 50%; 1 or 2 days = 0%. Cancellation on or after check-in is rejected. Unpaid PENDING/APPROVED cancellation has refund 0. Update Reservation and Payment refund/status fields atomically.
- Admin may override Reservation status and Payment status for the demo, but the operation must preserve valid status values and refund totals. `ReservationDAO.adminOverridePayment` checks status/refund consistency.

## SQL routines and triggers for Role 3

Use these MySQL callable signatures; Role 1's `ReservationDAO` calls them directly:

```sql
sp_create_reservation(
  IN p_guest_id BIGINT,
  IN p_room_id BIGINT,
  IN p_check_in DATE,
  IN p_check_out DATE,
  OUT p_reservation_id BIGINT,
  OUT p_total_price DECIMAL(10,2)
)

sp_cancel_reservation(
  IN p_reservation_id BIGINT,
  IN p_guest_id BIGINT,
  OUT p_refund_amount DECIMAL(10,2)
)

sp_expire_overdue(OUT p_cancelled_count INT)
```

Required triggers (two):

1. `BEFORE INSERT ON Reservation`: reject invalid dates and any overlap against blocking statuses.
2. `AFTER INSERT ON Payment`: reject if the linked Reservation is not `APPROVED` or has fewer than 7 days before check-in; otherwise set it to `CONFIRMED`.

Role 3 owns the cursor NOT FOUND handler, transaction/rollback strategy, and SQL-level error messages. Routines should own their transaction boundaries; Java callers must not start a competing transaction. Include executable SQL examples for overlap rejection, approval/payment transition, all refund boundaries (8, 7, 3, 2 days), and expiry at the 7-day cutoff.

## HTTP/session contract for Roles 1, 4, and 5

All JSPs are under `src/main/webapp/WEB-INF/views`; browser URLs go through servlets.

| Purpose | Method/path | Form fields | Session/access | Result |
|---|---|---|---|---|
| Guest registration | GET/POST `/register` | `name`, `email`, `phone`, `password` | Public; always creates GUEST | Register view, then `/login` |
| Login | GET/POST `/login` | `email`, `password` | Public | Set `user_id` (Long), `role` (`ADMIN`/`GUEST`), `email`; redirect by role |
| Logout | POST `/logout` | none | Any signed-in user | Invalidate session, redirect `/login` |
| Guest dashboard | GET `/guest/dashboard` | none | GUEST | Guest dashboard |
| Room search | GET `/guest/search` | ISO `checkIn`, `checkOut`, optional `typeId` | GUEST | Available results with room-type price |
| Create reservation | POST `/guest/reservations` | `roomId`, `checkIn`, `checkOut` | GUEST; guest id comes only from session | Receipt with id/quoted total |
| My bookings | GET `/guest/my-bookings` | none | GUEST; guest id comes only from session | Current guest's reservations |
| Pay | POST `/guest/payment` | `reservationId` | GUEST; owner checked server-side; amount loaded from DB | Payment trigger confirms eligible booking |
| Cancel | POST `/guest/cancel` | `reservationId` | GUEST; owner checked server-side | Refund amount/status shown |
| Profile | GET/POST `/guest/profile` | `name`, `email`, `phone` | GUEST; id comes only from session | Own profile only |
| Admin dashboard | GET `/admin/dashboard` | none | ADMIN | Admin dashboard |
| Room inventory | GET/POST `/admin/rooms` | `roomNumber`, `typeId`, `floor`, `roomId` | ADMIN | Room CRUD |
| Room type price | POST `/admin/room-types/price` | `typeId`, `pricePerNight` | ADMIN | Updated rate |
| Reservation approval/override | GET/POST `/admin/reservations` | `reservationId`, allowed status/action | ADMIN | Updated status |
| Guest directory | GET/POST `/admin/guests` | `guestId`, `name`, `email`, `phone` | ADMIN | View/update guest contact data |
| Expiry job | POST `/admin/expire-overdue` | none | ADMIN | Calls `sp_expire_overdue`; displays count |

`RoleFilter` enforces `/admin/*` and `/guest/*`. Never accept guest id, role, total price, or refund amount as authoritative browser input. Escape user content in JSP output and validate every field server-side.

## Role deliverables and handoffs

- Role 1: DAO/JNDI/authentication/input validation/session filter; keep SQL mappings aligned with this file.
- Role 2: ERD, five-table DDL, the two views and indexes, DB accounts/GRANTs, hash-only seeds, DTO beans; confirm the two schema amendments (`total_price`, `payment_status`).
- Role 3: two triggers, three routines, one cursor, query interface table, transaction/errors, SQL examples and boundary evidence.
- Role 4: admin dashboard, room/type pricing management, reservation approval/status override, guest directory, expiry control; use admin routes and role checks.
- Role 5: guest registration view, dashboard, date/type search, reservation/payment/cancel, own-profile edit; use guest routes and session-owned ids.

Each pull request should include its owned source files, a short build/run note, and any changed interface. Before merging an interface change, update this file and the affected DAO/servlet code in the same PR.
