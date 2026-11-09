<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, dao.ReservationDAO" %>
<%
    List<ReservationDAO.AdminReservationRow> reservations =
        (List<ReservationDAO.AdminReservationRow>) request.getAttribute("reservations");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html>
<head>
  <title>Reservations</title>
</head>
<body>
  <h1>Reservations</h1>
  <a href="<%= request.getContextPath() %>/admin/dashboard">Back to dashboard</a>

  <% if (error != null) { %>
    <p style="color:red"><%= error %></p>
  <% } %>

  <table border="1">
    <tr>
      <th>#</th><th>Guest</th><th>Room</th><th>Check-in</th><th>Check-out</th>
      <th>Total</th><th>Status</th><th>Actions</th>
    </tr>
    <% if (reservations != null) {
         for (ReservationDAO.AdminReservationRow r : reservations) { %>
      <tr>
        <td><%= r.reservationId() %></td>
        <td><%= r.guestName() %></td>
        <td><%= r.roomNumber() %></td>
        <td><%= r.checkIn() %></td>
        <td><%= r.checkOut() %></td>
        <td>$<%= r.totalPrice() %></td>
        <td><%= r.status() %></td>
        <td>
          <% if ("PENDING".equals(r.status())) { %>
            <form method="post" action="<%= request.getContextPath() %>/admin/reservations">
              <input type="hidden" name="action" value="approve">
              <input type="hidden" name="reservationId" value="<%= r.reservationId() %>">
              <button type="submit">Approve</button>
            </form>
          <% } %>
        </td>
      </tr>
    <%   }
       } %>
  </table>
</body>
</html>