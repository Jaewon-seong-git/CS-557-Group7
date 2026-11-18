# Guest UI (Role 5) DAO & Route Handoff

`GuestDashboardServlet` (`/guest/dashboard`) and `WEB-INF/views/guest/dashboard.jsp` are already committed to `main`. Please do **not** edit those two files.

## 1. Getting Guest ID from Session (Required for every Guest Servlet)
```java
HttpSession session = request.getSession(false);
if (session == null || !(session.getAttribute("user_id") instanceof Long guestId)
        || !"GUEST".equals(session.getAttribute("role"))) {
    response.sendRedirect(request.getContextPath() + "/login");
    return;
}
```

## 2. Room Search (`GET /guest/search` -> `RoomSearchServlet`)
- Load room types for dropdown: `List<RoomDAO.RoomTypeRow> types = roomDAO.getAllRoomTypes();`
- Validate `checkIn` is at least 7 days from `LocalDate.now()` and `checkOut.isAfter(checkIn)`.
- Search rooms: `List<RoomDAO.RoomRow> rooms = roomDAO.searchAvailableRooms(checkIn, checkOut, typeId);` (`typeId` can be `null`).

## 3. Bookings, Payment & Cancel (`GuestReservationServlet`)
- **My Bookings (`GET /guest/bookings`):**
  `List<ReservationDAO.BookingRow> bookings = reservationDAO.findBookingsForGuest(guestId);`
- **Create Booking (`POST /guest/reservations`, action=`create`):**
  `ReservationDAO.ReservationReceipt receipt = reservationDAO.createReservation(guestId, roomId, checkIn, checkOut);`
- **Pay (`POST /guest/reservations`, action=`pay`):**
  `boolean paid = reservationDAO.recordPayment(reservationId, guestId);`
- **Cancel (`POST /guest/reservations`, action=`cancel`):**
  `ReservationDAO.CancellationResult res = reservationDAO.cancelReservation(reservationId, guestId);`
  *(Note: `ReservationDAO` verifies `guestId` ownership and throws `SecurityException` if tampered.)*
